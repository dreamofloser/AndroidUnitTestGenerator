package com.example.androidapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.content.res.Resources;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ResourceLabelFormatterTest {

    private Resources resources;
    private ResourceLabelFormatter target;

    @Before
    public void setUp() {
        resources = mock(Resources.class);
        target = new ResourceLabelFormatter(resources);
    }

    @Test
    public void appName_withMockedResources_shouldUseDependency() throws Exception {
        when(resources.getString(R.string.app_name)).thenReturn("sample");

        String result = target.appName();

        assertEquals("sample", result);

        verify(resources).getString(R.string.app_name);
    }

    @Test
    public void welcomeMessage_withMockedResources_shouldUseDependency() throws Exception {
        when(resources.getString(R.string.welcome_message, "sample")).thenReturn("sample");

        String result = target.welcomeMessage("sample");

        assertEquals("sample", result);

        verify(resources).getString(R.string.welcome_message, "sample");
    }

}
