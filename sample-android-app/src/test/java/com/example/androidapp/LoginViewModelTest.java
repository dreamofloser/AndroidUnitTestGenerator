package com.example.androidapp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LoginRepository repository;
    private LoginViewModel target;

    @Before
    public void setUp() {
        repository = mock(LoginRepository.class);
        target = new LoginViewModel(repository);
    }

    @Test
    public void login_withMockedRepository_shouldUseDependency() throws Exception {
        when(repository.login("sample", "sample")).thenReturn(true);

        boolean result = target.login("sample", "sample");

        assertTrue(result);

        verify(repository).login("sample", "sample");
    }

    @Test
    public void getDisplayName_shouldRunWithoutException() throws Exception {
        LiveData<String> result = target.getDisplayName();

        assertNotNull(result);
    }

    @Test
    public void loadDisplayName_withMockedRepository_shouldUseDependency() throws Exception {
        target.loadDisplayName("sample");

        verify(repository).loadDisplayName("sample");
    }

}
