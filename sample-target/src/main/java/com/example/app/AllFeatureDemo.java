package com.example.app;

public class AllFeatureDemo {
    private final DemoRepository repository;

    public AllFeatureDemo(DemoRepository repository) {
        this.repository = repository;
    }

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public boolean isAdult(int age) {
        return age >= 18;
    }

    public String normalizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }

        return name;
    }

    public void requirePositive(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
    }

    public String loadUserName(String id) {
        return repository.getUserName(id);
    }

    public boolean userExists(String id) {
        return repository.exists(id);
    }

    public void saveUserName(String id, String name) {
        repository.saveUserName(id, name);
    }
}

interface DemoRepository {
    String getUserName(String id);

    boolean exists(String id);

    void saveUserName(String id, String name);
}
