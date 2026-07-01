package com.example.app;

public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public String getUserName(String id) {
        return repository.getUserName(id);
    }

    public boolean isRegistered(String id) {
        return repository.exists(id);
    }

    public void saveUserName(String id, String name) {
        repository.saveUserName(id, name);
    }
}
