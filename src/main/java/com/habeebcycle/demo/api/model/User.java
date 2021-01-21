package com.habeebcycle.demo.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.io.Serializable;

public class User implements Serializable {

    @Id
    private String id;

    @Version
    private int version; // Used for data optimistic lock

    private String username; // We will make this to be unique
    private String email; // We will make this to be unique
    private String name;

    public User() {
    }

    public User(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
