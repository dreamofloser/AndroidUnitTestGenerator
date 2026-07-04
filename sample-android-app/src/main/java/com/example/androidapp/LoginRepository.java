package com.example.androidapp;

public interface LoginRepository {
    boolean login(String username, String password);

    String loadDisplayName(String userId);
}
