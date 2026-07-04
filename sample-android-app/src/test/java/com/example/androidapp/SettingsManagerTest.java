package com.example.androidapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.content.SharedPreferences;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsManagerTest {

    private SharedPreferences preferences;
    private SettingsManager target;

    @Before
    public void setUp() {
        preferences = mock(SharedPreferences.class);
        target = new SettingsManager(preferences);
    }

    @Test
    public void isFirstLaunch_withMockedPreferences_shouldUseDependency() throws Exception {
        when(preferences.getBoolean("first_launch", true)).thenReturn(true);

        boolean result = target.isFirstLaunch();

        assertTrue(result);

        verify(preferences).getBoolean("first_launch", true);
    }

    @Test
    public void getToken_withMockedPreferences_shouldUseDependency() throws Exception {
        when(preferences.getString("token", "")).thenReturn("sample");

        String result = target.getToken();

        assertEquals("sample", result);

        verify(preferences).getString("token", "");
    }

}
