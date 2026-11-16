package com.elearning.smartflash.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.elearning.smartflash.data.remote.dto.AuthResponseDto;
import com.elearning.smartflash.data.repository.AuthRepository;
import com.elearning.smartflash.util.Resource;

/**
 * ViewModel for Splash, Login, Register, and Forgot-Password OTP flows.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Resource<Boolean>> verifySession() {
        return authRepository.verifySession();
    }

    public LiveData<Resource<AuthResponseDto>> login(String identifier, String password) {
        return authRepository.login(identifier, password);
    }

    public LiveData<Resource<AuthResponseDto>> register(
            String username,
            String email,
            String password,
            String fullName,
            String role
    ) {
        return authRepository.register(username, email, password, fullName, role);
    }

    public LiveData<Resource<AuthResponseDto>> loginWithGoogle(String idToken) {
        return authRepository.loginWithGoogle(idToken);
    }

    public LiveData<Resource<String>> forgotPassword(String email) {
        return authRepository.forgotPassword(email);
    }

    public LiveData<Resource<String>> resetPassword(String email, String otpCode, String newPassword) {
        return authRepository.resetPassword(email, otpCode, newPassword);
    }
}
