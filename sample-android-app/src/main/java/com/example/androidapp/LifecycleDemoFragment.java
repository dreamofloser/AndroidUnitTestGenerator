package com.example.androidapp;

import android.app.Fragment;
import android.os.Bundle;

public class LifecycleDemoFragment extends Fragment {
    private boolean created;
    private boolean started;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        created = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        started = true;
    }

    public boolean isCreated() {
        return created;
    }

    public boolean isStarted() {
        return started;
    }
}
