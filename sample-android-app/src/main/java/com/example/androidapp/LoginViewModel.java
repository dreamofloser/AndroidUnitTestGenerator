package com.example.androidapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    private final LoginRepository repository;
    private final MutableLiveData<String> displayName = new MutableLiveData<>();

    public LoginViewModel(LoginRepository repository) {
        this.repository = repository;
    }

    public boolean login(String username, String password) {
        return repository.login(username, password);
    }

    public LiveData<String> getDisplayName() {
        return displayName;
    }

    public void loadDisplayName(String userId) {
        displayName.setValue(repository.loadDisplayName(userId));
    }
}
