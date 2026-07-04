package com.example.androidapp;

import android.app.Activity;
import android.os.Bundle;

public class LifecycleDemoActivity extends Activity {
    private boolean created;
    private boolean started;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        created = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
    }

    public boolean isCreated() {
        return created;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isResumed() {
        return resumed;
    }
}
