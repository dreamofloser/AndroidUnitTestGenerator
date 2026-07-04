package com.example.androidapp;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LifecycleDemoFragmentTest {

    @Test
    public void fragment_shouldAttachAndStart() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).create().start().resume();
        Activity activity = controller.get();
        LifecycleDemoFragment fragment = new LifecycleDemoFragment();

        activity.getFragmentManager().beginTransaction().add(fragment, "target").commitNow();

        assertNotNull(fragment);
        assertTrue(fragment.isAdded());
        assertTrue(fragment.isCreated());
        assertTrue(fragment.isStarted());

        controller.pause().stop().destroy();
    }

}
