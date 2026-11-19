package com.elearning.smartflash.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.databinding.ActivitySplashBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.Resource;

/**
 * Entry splash screen that checks stored JWT session and routes to MainActivity or LoginActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DELAY_MS = 600L;

    private ActivitySplashBinding binding;
    private AuthViewModel authViewModel;
    private TokenManager tokenManager;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthInputHelper.applyWhiteStatusBar(this);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, MIN_SPLASH_DELAY_MS);
    }

    private void checkAuthAndNavigate() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!tokenManager.isLoggedIn()) {
            tokenManager.setGuestMode(false);
            navigateToWelcome();
            return;
        }

        authViewModel.verifySession().observe(this, resource -> {
            if (resource == null || resource.getStatus() == Resource.Status.LOADING || navigated) {
                return;
            }
            if (Boolean.TRUE.equals(resource.getData())) {
                navigateToMain();
            } else {
                navigateToWelcome();
            }
        });
    }

    private void navigateToMain() {
        if (navigated) return;
        navigated = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToWelcome() {
        if (navigated) return;
        navigated = true;
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}
