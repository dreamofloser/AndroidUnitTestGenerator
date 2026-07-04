package com.example.androidapp;

import android.content.Intent;

public class ShareIntentFactory {
    public Intent createShareIntent(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
    }

    public boolean isShareIntent(Intent intent) {
        return Intent.ACTION_SEND.equals(intent.getAction());
    }
}
