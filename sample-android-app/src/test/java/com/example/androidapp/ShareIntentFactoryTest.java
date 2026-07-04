package com.example.androidapp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.content.Intent;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShareIntentFactoryTest {

    @Test
    public void createShareIntent_withDefaultInputs_shouldRun() throws Exception {
        ShareIntentFactory target = new ShareIntentFactory();

        Intent result = target.createShareIntent("sample");

        assertNotNull(result);
    }

    @Test
    public void createShareIntent_whenTextIsEmpty_shouldRun() throws Exception {
        ShareIntentFactory target = new ShareIntentFactory();

        Intent result = target.createShareIntent("");

        assertNotNull(result);
    }

    @Test
    public void createShareIntent_whenTextIsNull_shouldRun() throws Exception {
        ShareIntentFactory target = new ShareIntentFactory();

        Intent result = target.createShareIntent(null);

        assertNotNull(result);
    }

    @Test
    public void isShareIntent_withDefaultInputs_shouldRun() throws Exception {
        ShareIntentFactory target = new ShareIntentFactory();

        boolean result = target.isShareIntent(new Intent());
    }

}
