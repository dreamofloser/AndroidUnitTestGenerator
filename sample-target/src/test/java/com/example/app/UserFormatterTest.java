package com.example.app;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class UserFormatterTest {

    @Test
    public void displayName_shouldRunWithoutException() throws Exception {
        UserFormatter target = new UserFormatter();

        String result = target.displayName("sample");

        assertNotNull(result);
    }

}
