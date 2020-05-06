package com.example.chat.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    public enum AuthenticationState {
        UNAUTHENTICATED,
        AUTHENTICATED,
        INVALID_AUTHENTICATION
    }

    final MutableLiveData<AuthenticationState> authenticationState = new MutableLiveData<>();
    String username;

    public LoginViewModel() {
        authenticationState.setValue(AuthenticationState.AUTHENTICATED);
        username = "";
    }

    public void authenticate(String username, String password) {
        if (passwordIsValidForUsername(username, password)) {
            this.username = username;
            authenticationState.setValue(AuthenticationState.AUTHENTICATED);
        } else {
            authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
        }
    }

    public void refuseAuthentication() {
        authenticationState.setValue(AuthenticationState.UNAUTHENTICATED);
    }

    private boolean passwordIsValidForUsername(String username, String password) {
        // TODO
        return true;
    }
}
