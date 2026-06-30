package com.example.app;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class UserFormatterTest {

    @Test
    public void displayName_withDefaultInputs_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        String result = target.displayName("sample");

        assertNotNull(result);
    }

    @Test
    public void displayName_whenNameIsEmpty_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        String result = target.displayName("");

        assertNotNull(result);
    }

    @Test
    public void displayName_whenNameIsNull_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        String result = target.displayName(null);

        assertNotNull(result);
    }

    @Test
    public void hasName_withDefaultInputs_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        boolean result = target.hasName("sample");
    }

    @Test
    public void hasName_whenNameIsEmpty_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        boolean result = target.hasName("");
    }

    @Test
    public void hasName_whenNameIsNull_shouldRun() throws Exception {
        UserFormatter target = new UserFormatter();

        boolean result = target.hasName(null);
    }

}
