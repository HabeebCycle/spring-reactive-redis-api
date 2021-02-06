package com.habeebcycle.demo.api.persistence;

import com.habeebcycle.demo.api.model.User;
import com.habeebcycle.demo.api.persistence.UserRepoImpl;
import com.habeebcycle.demo.api.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;
import redis.embedded.RedisServer;

//@DataRedisTest(properties = {"spring.redis.password="})
@SpringBootTest(properties = {"spring.redis.password="})
public class  PersistenceTests {

    private final static RedisServer REDISSERVER = new RedisServer(6379);

    @Autowired
    private UserRepository repository;

    // Variable used to create a new entity before each test.
    private User savedUser;

    @BeforeAll
    static void startUpRedisServer() {
        REDISSERVER.start();
    }

    @AfterAll
    static void shutDownRedisServer() {
        REDISSERVER.stop();
    }

    @BeforeEach
    void setUpDB() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();

        User user = new User("username", "email", "name");

        // Verify that we can save, store the created user into the savedUser variable
        // and compare the saved user.
        StepVerifier.create(repository.save(user))
                .expectNextMatches(createdUser -> {
                    savedUser = createdUser;
                    return assertEqualUser(user, savedUser);
                }).verifyComplete();

        // Verify the number of entities in the database
        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void createTest() {
        User userA = new User("username-1", "email-1", "name");

        // Verify that we can save and compare the saved user
        StepVerifier.create(repository.save(userA))
                .expectNextMatches(createdUser ->
                        userA.getId() != null && createdUser.getId().equals(userA.getId()))
                .verifyComplete();

        // Verify we can get back the User by using findById method
        StepVerifier.create(repository.findById(userA.getId()))
                .expectNextMatches(foundUser -> assertEqualUser(userA, foundUser))
                .verifyComplete();

        // Save without username and verify that it fails
        User userB = new User("", "emailB@email.com", "name");
        StepVerifier.create(repository.save(userB))
                .expectError(IllegalArgumentException.class)
                .verify();

        // Save without username and verify that it fails
        User userC = new User("usernameB", "", "name");
        StepVerifier.create(repository.save(userC))
                .expectError(IllegalArgumentException.class)
                .verify();

        //Verify that the database has only savedUser & userA
        StepVerifier.create(repository.count())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void updateTest() {
        String newName = "name-update";
        savedUser.setName(newName);

        // Verify that we can update and compare the saved user new name
        StepVerifier.create(repository.save(savedUser))
                .expectNextMatches(updatedUser -> updatedUser.getId().equals(savedUser.getId()) &&
                       updatedUser.getName().equals(newName) && updatedUser.getVersion() == 1)
                .verifyComplete();

        // Verify that we can update and compare the saved user new username
        String newUsername = "username-update";
        savedUser.setUsername(newUsername);

        StepVerifier.create(repository.save(savedUser))
                .expectNextMatches(updatedUser -> updatedUser.getId().equals(savedUser.getId()) &&
                        updatedUser.getUsername().equals(newUsername) && updatedUser.getVersion() == 2)
                .verifyComplete();

        // Verify that we still have 1 entity in the database
        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void deleteTest() {
        // Verify that we can delete the saved user
        StepVerifier.create(repository.delete(savedUser)).verifyComplete();

        // Verify that the saved user has been deleted
        StepVerifier.create(repository.existsById(savedUser.getId()))
                .expectNext(false)
                .verifyComplete();

        // This should also work since delete is an idempotent operation
        StepVerifier.create(repository.deleteById(savedUser.getId())).verifyComplete();

        // Verify that we have no entity in the database
        StepVerifier.create(repository.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void getByUsernameAndEmailTest() {
        // Verify that we can get the saved user by username
        StepVerifier.create(repository.findByUsername(savedUser.getUsername()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();

        // Verify that we can get the saved user by email
        StepVerifier.create(repository.findByEmail(savedUser.getEmail()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();

        // Verify that we still have 1 entity in the database
        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void duplicateErrorTest() {
        // Same username will fail because username should be unique
        User user = new User("username", "email-1", "name"); //using the same username as savedUser

        // Verify that we have error due to duplicate username
        StepVerifier.create(repository.save(user))
                .expectError(DuplicateKeyException.class)
                .verify();

        // Same email will fail because email should be unique
        user = new User("username-1", "email", "name"); //using the same email as savedUser

        // Verify that we have error due to duplicate email
        StepVerifier.create(repository.save(user))
                .expectError(DuplicateKeyException.class)
                .verify();

        // Add a new user and verify that it saves
        User newUser = new User("username-2", "email-2", "name-2");
        StepVerifier.create(repository.save(newUser))
                .expectNextMatches(createdUser -> assertEqualUser(newUser, createdUser))
                .verifyComplete();

        // Verify that we only have 2 entities in the database
        StepVerifier.create(repository.count())
                .expectNext(2L)
                .verifyComplete();

        // Update savedUser with the above username-2 and verify duplicate error
        savedUser.setUsername("username-2");
        StepVerifier.create(repository.save(savedUser))
                .expectError(DuplicateKeyException.class)
                .verify();

        // Verify that username and version didn't change
        StepVerifier.create(repository.findById(savedUser.getId()))
                .expectNextMatches(foundUser -> foundUser.getUsername().equals("username")
                                        && foundUser.getVersion() == 0)
                .verifyComplete();
    }

    @Test
    void optimisticLockErrorTest() {

        // Store the saved user in two separate objects
        User user1 = repository.findById(savedUser.getId()).block(); // Wait by blocking the thread
        User user2 = repository.findById(savedUser.getId()).block(); // Wait by blocking the thread

        Assertions.assertNotNull(user1); // Assert it is not null
        Assertions.assertNotNull(user2); // Assert it is not null
        Assertions.assertEquals(user1.getVersion(), user2.getVersion()); // Assert both version are same
        assertEqualUser(user1, user2);

        String newName1 = "New Name Object1";
        String newName2 = "New Name Object2";

        // Update the user using the first user object. THIS WILL WORK
        user1.setName(newName1);
        StepVerifier.create(repository.save(user1))
                .expectNextMatches(updatedUser -> updatedUser.getVersion() == 1)
                .verifyComplete();

        // Update the user using the second object.
        // This should FAIL since this second object now holds an old version number, i.e. an Optimistic Lock
        user2.setName(newName2);
        StepVerifier.create(repository.save(user2))
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        // Get the updated user from the database and verify its new state
        StepVerifier.create(repository.findById(savedUser.getId()))
                .expectNextMatches(foundUser ->
                    foundUser.getVersion() == 1 &&
                            foundUser.getName().equals(newName1))
                .verifyComplete();

        // Verify we still have one user in the database
        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    // Personal method used in the tests above to compare the User entity.
    private boolean assertEqualUser(User expectedUser, User actualUser) {
        Assertions.assertEquals(expectedUser.getId(), actualUser.getId());
        Assertions.assertEquals(expectedUser.getUsername(), actualUser.getUsername());
        Assertions.assertEquals(expectedUser.getEmail(), actualUser.getEmail());
        Assertions.assertEquals(expectedUser.getName(), actualUser.getName());

        return (expectedUser.getId().equals(actualUser.getId())) &&
                (expectedUser.getUsername().equals(actualUser.getUsername())) &&
                (expectedUser.getEmail().equals(actualUser.getEmail())) &&
                (expectedUser.getName().equals(actualUser.getName())) &&
                (expectedUser.getVersion() == actualUser.getVersion());
    }
}
