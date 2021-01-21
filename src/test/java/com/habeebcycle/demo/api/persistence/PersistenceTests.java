package com.habeebcycle.demo.api.persistence;

import com.habeebcycle.demo.api.model.User;
import com.habeebcycle.demo.api.persistence.UserRepoImpl;
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
public class PersistenceTests {

    private final static RedisServer REDISSERVER = new RedisServer(6379);

    @Autowired
    private UserRepoImpl repository;

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

        StepVerifier.create(repository.save(user))
                .expectNextMatches(createdUser -> {
                    savedUser = createdUser;
                    return assertEqualUser(user, savedUser);
                }).verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void createTest() {
        User user = new User("username-1", "email-1", "name");

        StepVerifier.create(repository.save(user))
                .expectNextMatches(createdUser ->
                        user.getId() != null && createdUser.getId().equals(user.getId()))
                .verifyComplete();

        StepVerifier.create(repository.findById(user.getId()))
                .expectNextMatches(foundUser -> assertEqualUser(user, foundUser))
                .verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void updateTest() {
        String newName = "name-update";
        savedUser.setName(newName);

        StepVerifier.create(repository.save(savedUser))
                .expectNextMatches(updatedUser -> updatedUser.getId().equals(savedUser.getId()) &&
                       updatedUser.getName().equals(newName) && updatedUser.getVersion() == 1)
                .verifyComplete();

        String newUsername = "username-update";
        savedUser.setUsername(newUsername);

        StepVerifier.create(repository.save(savedUser))
                .expectNextMatches(updatedUser -> updatedUser.getId().equals(savedUser.getId()) &&
                        updatedUser.getUsername().equals(newUsername) && updatedUser.getVersion() == 2)
                .verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void deleteTest() {
        StepVerifier.create(repository.delete(savedUser)).verifyComplete();

        StepVerifier.create(repository.existsById(savedUser.getId()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void getByUsernameAndEmailTest() {
        StepVerifier.create(repository.findByUsername(savedUser.getUsername()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();

        StepVerifier.create(repository.findByEmail(savedUser.getEmail()))
                .expectNextMatches(foundUser -> assertEqualUser(savedUser, foundUser))
                .verifyComplete();

        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void duplicateErrorTest() {
        // Same username will fail because username should be unique
        User user = new User("username", "email-1", "name");

        StepVerifier.create(repository.save(user))
                .expectError(DuplicateKeyException.class)
                .verify();

        // Same email will fail because email should be unique
        user = new User("username-1", "email", "name");

        StepVerifier.create(repository.save(user))
                .expectError(DuplicateKeyException.class)
                .verify();

        StepVerifier.create(repository.count())
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void optimisticLockErrorTest() {

        // Store the saved user in two separate objects
        User user1 = repository.findById(savedUser.getId()).block();
        User user2 = repository.findById(savedUser.getId()).block();

        Assertions.assertNotNull(user1);
        Assertions.assertNotNull(user2);
        Assertions.assertEquals(user1.getVersion(), user2.getVersion());
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
