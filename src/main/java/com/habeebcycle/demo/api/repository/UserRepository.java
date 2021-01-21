package com.habeebcycle.demo.api.repository;

import com.habeebcycle.demo.api.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String> {

    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String name);
    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
}
