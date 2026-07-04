package com.example.androidapp;

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
public class LifecycleDemoActivityTest {

    @Test
    public void activity_shouldMoveThroughLifecycle() {
        ActivityController<LifecycleDemoActivity> controller = Robolectric.buildActivity(LifecycleDemoActivity.class);

        LifecycleDemoActivity activity = controller.create().start().resume().get();

        assertNotNull(activity);
        assertTrue(activity.isCreated());
        assertTrue(activity.isStarted());
        assertTrue(activity.isResumed());

        controller.pause().stop().destroy();
    }

}
