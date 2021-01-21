package com.habeebcycle.demo.api.persistence;

import com.habeebcycle.demo.api.model.User;
import com.habeebcycle.demo.api.repository.UserRepository;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class UserRepoImpl implements UserRepository {

    private final static String KEY = "USERS";

    private final ReactiveRedisOperations<String, User> redisOperations;
    private final ReactiveHashOperations<String, String, User> hashOperations;

    @Autowired
    public UserRepoImpl(ReactiveRedisOperations<String, User> redisOperations) {
        this.redisOperations = redisOperations;
        this.hashOperations = redisOperations.opsForHash();
    }

    @Override
    public Mono<User> findById(String id) {
        return hashOperations.get(KEY, id);
    }

    @Override
    public Flux<User> findAll() {
        return hashOperations.values(KEY);
    }

    @Override
    public Mono<User> save(User user) {
        if(user.getUsername().isEmpty() || user.getEmail().isEmpty())
            return Mono.error(new IllegalArgumentException("Cannot be saved: username and email are required, but one or both is empty."))
                    .thenReturn(user);

        if (user.getId() == null || user.getId().isEmpty()) {
            return Mono.defer(() -> addNewUser(user));
        } else {
            return findById(user.getId())
                    .flatMap(u -> {
                        if (u.getVersion() != user.getVersion()) {
                            return Mono.error(
                                    new OptimisticLockingFailureException(
                                            "This record has already been updated earlier by another object."));
                        } else {
                            user.setVersion(user.getVersion() + 1);
                            return hashOperations.put(KEY, user.getId(), user)
                                    .map(isSaved -> user);
                        }
                    })
                    .switchIfEmpty(Mono.defer(() -> addNewUser(user)));
        }
    }

    // private utility method to add new user if not exist with username and email
    private Mono<User> addNewUser(User user) {

        //email and username should be unique
        return existsByUsername(user.getUsername())
                .mergeWith(existsByEmail(user.getEmail()))
                .any(b -> b)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new DuplicateKeyException("Duplicate key, Username: " +
                                user.getUsername() + " or Email: " + user.getEmail() + " exists."));
                    } else {
                        String userId = UUID.randomUUID().toString().replaceAll("-", "");
                        user.setId(userId);
                        user.setVersion(0);
                        return hashOperations.put(KEY, user.getId(), user)
                                .map(isSaved -> user);
                    }
                })
                .thenReturn(user);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return hashOperations.values(KEY)
                .filter(u -> u.getUsername().equals(username))
                .singleOrEmpty();
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return hashOperations.values(KEY)
                .filter(u -> u.getEmail().equals(email))
                .singleOrEmpty();
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        return hashOperations.hasKey(KEY, id);
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return findByUsername(username)
                .hasElement();
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return findByEmail(email)
                .hasElement();
    }

    @Override
    public Mono<Long> count() {
        return hashOperations.values(KEY).count();
    }

    @Override
    public Mono<Void> deleteAll() {
        return hashOperations.delete(KEY).then();
    }

    @Override
    public Mono<Void> delete(User user) {
        return hashOperations.remove(KEY, user.getId()).then();
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return hashOperations.remove(KEY, id).then();
    }


    //Others... Implements the following methods for your business logic

    @Override
    public <S extends User> Flux<S> saveAll(Iterable<S> iterable) {
        return null;
    }

    @Override
    public <S extends User> Flux<S> saveAll(Publisher<S> publisher) {
        return null;
    }

    @Override
    public Mono<User> findById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Boolean> existsById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Flux<User> findAllById(Iterable<String> iterable) {
        return null;
    }

    @Override
    public Flux<User> findAllById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Void> deleteById(Publisher<String> publisher) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends User> iterable) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends User> publisher) {
        return null;
    }


}
