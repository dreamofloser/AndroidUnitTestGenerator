package com.example.app;

public class UserFormatter {
    private final String prefix;

    public UserFormatter() {
        this("User");
    }

    public UserFormatter(String prefix) {
        this.prefix = prefix;
    }

    public String displayName(String name) {
        if (name == null || name.isEmpty()) {
            return prefix + ": unknown";
        }

        return prefix + ": " + name;
    }
}
