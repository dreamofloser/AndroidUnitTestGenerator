package com.example.androidapp;

import android.content.SharedPreferences;

public class SettingsManager {
    private final SharedPreferences preferences;

    public SettingsManager(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean("first_launch", true);
    }

    public String getToken() {
        return preferences.getString("token", "");
    }
}
