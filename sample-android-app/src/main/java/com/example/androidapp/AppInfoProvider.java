package com.example.androidapp;

import android.content.Context;

public class AppInfoProvider {
    private final Context context;

    public AppInfoProvider(Context context) {
        this.context = context;
    }

    public String getAppName() {
        return context.getString(R.string.app_name);
    }
}
