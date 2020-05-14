package com.example.chat.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginViewModel extends ViewModel {
    private FirebaseAuth mAuth;

    private static final Pattern p = Pattern.compile("^(.+)@");

    final MutableLiveData<FirebaseUser> userMutableLiveData = new MutableLiveData<>();

    public enum AuthenticationState {
        UNAUTHENTICATED,
        AUTHENTICATED,
        INVALID_AUTHENTICATION
    }

    final MutableLiveData<AuthenticationState> authenticationState = new MutableLiveData<>();
    String username;

    public LoginViewModel() {
        mAuth = FirebaseAuth.getInstance();
        authenticationState.setValue(AuthenticationState.UNAUTHENTICATED);
        username = "";
    }

    public void authenticate(final String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
            return;
        }
        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    userMutableLiveData.setValue(user);
                    authenticationState.setValue(AuthenticationState.AUTHENTICATED);
                } else {
                    authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
                }
            }
        });
    }

    public void refuseAuthentication() {
        authenticationState.setValue(AuthenticationState.UNAUTHENTICATED);
    }

    public void createAccountAndLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
            return;
        }

        mAuth.createUserWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    userMutableLiveData.setValue(user);
                    Matcher m = p.matcher(user.getEmail());
                    if(m.find()) {
                        String dname = m.group(1);
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(dname)
                                .build();
                        user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    authenticationState.setValue(AuthenticationState.AUTHENTICATED);
                                }
                            }
                        });
                    }
                } else {
                    authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
                }
            }
        });
    }

    public String getDisplayName() {
        if (authenticationState.getValue() == AuthenticationState.AUTHENTICATED) return userMutableLiveData.getValue().getDisplayName();
        else return "PLACEHOLDER (not loggedin)";
    }

    public String getUid() {
        if (authenticationState.getValue() == AuthenticationState.AUTHENTICATED) return userMutableLiveData.getValue().getUid();
        else return "PLACEHOLDER (not loggedin)";
    }

    //    private boolean passwordIsValidForUsername(String username, String password) {
//        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(@NonNull Task<AuthResult> task) {
//                if (task.isSuccessful()) {
//                    FirebaseUser user = mAuth.getCurrentUser();
//                    userMutableLiveData.setValue(user);
//                    authenticationState.setValue(AuthenticationState.AUTHENTICATED);
//                } else {
//                    authenticationState.setValue(AuthenticationState.INVALID_AUTHENTICATION);
//                }
//            }
//        });
//    }
}
