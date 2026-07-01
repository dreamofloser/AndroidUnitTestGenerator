package com.example.app;

public interface UserRepository {
    String getUserName(String id);

    boolean exists(String id);

    void saveUserName(String id, String name);
}
