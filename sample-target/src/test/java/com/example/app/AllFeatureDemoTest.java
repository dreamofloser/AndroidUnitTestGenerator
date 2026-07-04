package com.example.app;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class AllFeatureDemoTest {

    private DemoRepository repository;
    private AllFeatureDemo target;

    @Before
    public void setUp() {
        repository = mock(DemoRepository.class);
        target = new AllFeatureDemo(repository);
    }

    @Test
    public void add_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        int result = target.add(1, 1);

        assertEquals(2, result);
    }

    @Test
    public void subtract_withDefaultInputs_shouldReturnExpectedValue() throws Exception {
        int result = target.subtract(1, 1);

        assertEquals(0, result);
    }

    @Test
    public void isAdult_whenAgeIs18_shouldReturnTrue() throws Exception {
        boolean result = target.isAdult(18);

        assertTrue(result);
    }

    @Test
    public void isAdult_whenAgeIs17_shouldReturnFalse() throws Exception {
        boolean result = target.isAdult(17);

        assertFalse(result);
    }

    @Test
    public void normalizeName_withDefaultInputs_shouldRun() throws Exception {
        String result = target.normalizeName("sample");

        assertNotNull(result);
    }

    @Test
    public void normalizeName_whenNameIsEmpty_shouldRun() throws Exception {
        String result = target.normalizeName("");

        assertNotNull(result);
    }

    @Test
    public void normalizeName_whenNameIsNull_shouldRun() throws Exception {
        String result = target.normalizeName(null);

        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requirePositive_whenInvalidInput_shouldThrowIllegalArgumentException() throws Exception {
        target.requirePositive(0);
    }

    @Test
    public void loadUserName_withMockedRepository_shouldUseDependency() throws Exception {
        when(repository.getUserName("sample")).thenReturn("sample");

        String result = target.loadUserName("sample");

        assertEquals("sample", result);

        verify(repository).getUserName("sample");
    }

    @Test
    public void userExists_withMockedRepository_shouldUseDependency() throws Exception {
        when(repository.exists("sample")).thenReturn(true);

        boolean result = target.userExists("sample");

        assertTrue(result);

        verify(repository).exists("sample");
    }

    @Test
    public void saveUserName_withMockedRepository_shouldUseDependency() throws Exception {
        target.saveUserName("sample", "sample");

        verify(repository).saveUserName("sample", "sample");
    }

}
