package com.example.app;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class UserServiceTest {

    private UserRepository repository;
    private UserService target;

    @Before
    public void setUp() {
        repository = mock(UserRepository.class);
        target = new UserService(repository);
    }

    @Test
    public void getUserName_withMockedRepository_shouldUseDependency() throws Exception {
        when(repository.getUserName("sample")).thenReturn("sample");

        String result = target.getUserName("sample");

        assertEquals("sample", result);

        verify(repository).getUserName("sample");
    }

    @Test
    public void isRegistered_withMockedRepository_shouldUseDependency() throws Exception {
        when(repository.exists("sample")).thenReturn(true);

        boolean result = target.isRegistered("sample");

        assertTrue(result);

        verify(repository).exists("sample");
    }

    @Test
    public void saveUserName_withMockedRepository_shouldUseDependency() throws Exception {
        target.saveUserName("sample", "sample");

        verify(repository).saveUserName("sample", "sample");
    }

}
