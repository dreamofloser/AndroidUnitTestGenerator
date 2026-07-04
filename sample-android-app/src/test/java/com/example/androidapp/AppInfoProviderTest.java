package com.example.androidapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.content.Context;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppInfoProviderTest {

    private Context context;
    private AppInfoProvider target;

    @Before
    public void setUp() {
        context = mock(Context.class);
        target = new AppInfoProvider(context);
    }

    @Test
    public void getAppName_withMockedContext_shouldUseDependency() throws Exception {
        when(context.getString(R.string.app_name)).thenReturn("sample");

        String result = target.getAppName();

        assertEquals("sample", result);

        verify(context).getString(R.string.app_name);
    }

}
