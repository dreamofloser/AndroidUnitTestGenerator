package com.example.androidapp;

import android.content.res.Resources;

public class ResourceLabelFormatter {
    private final Resources resources;

    public ResourceLabelFormatter(Resources resources) {
        this.resources = resources;
    }

    public String appName() {
        return resources.getString(R.string.app_name);
    }

    public String welcomeMessage(String name) {
        return resources.getString(R.string.welcome_message, name);
    }
}
