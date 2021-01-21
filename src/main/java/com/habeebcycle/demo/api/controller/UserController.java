package com.habeebcycle.demo.api.controller;

import com.habeebcycle.demo.api.model.User;
import com.habeebcycle.demo.api.service.UserService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{userId}")
    public Mono<User> getUserById(@PathVariable String userId) {
        return userService.getUserById(userId);
    }

    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    @PutMapping("/{userId}")
    public Mono<User> updateUser(@PathVariable String userId, @RequestBody User user) {
        if(user.getId() == null || user.getId().isEmpty()) {
            user.setId(userId);
        }
        return userService.saveUser(user);
    }

    @DeleteMapping("/{userId}")
    public Mono<Void> deleteUserById(@PathVariable String userId) {
        return userService.deleteUserById(userId);
    }

    @DeleteMapping
    public Mono<Void> deleteAllUsers() {
        return userService.deleteAllUsers();
    }
}
