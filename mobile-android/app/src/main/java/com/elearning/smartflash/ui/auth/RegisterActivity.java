package com.elearning.smartflash.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.databinding.ActivityRegisterBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

/**
 * Quizlet-style sign-up landing screen (Image 1) offering Google Sign-In
 * and Email registration (routing to CreateAccountActivity).
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private TokenManager tokenManager;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    GoogleSignInHelper.handleSignInResult(result.getData(), new GoogleSignInHelper.Callback() {
                        @Override
                        public void onIdTokenReady(String idToken) {
                            submitGoogleLogin(idToken);
                        }

                        @Override
                        public void onError(String message) {
                            showErrorBanner(message);
                        }
                    })
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthInputHelper.applyWhiteStatusBar(this);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnGoogleRegister.setOnClickListener(v -> startGoogleSignIn());

        binding.btnEmailRegister.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class))
        );

        binding.tabGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void startGoogleSignIn() {
        hideErrorBanner();
        if (!GoogleSignInHelper.isConfigured(this)) {
            showErrorBanner("Chưa cấu hình google_web_client_id trong strings.xml.");
            return;
        }
        GoogleSignInClient client = GoogleSignInHelper.buildClient(this);
        client.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(client.getSignInIntent())
        );
    }

    private void submitGoogleLogin(String idToken) {
        authViewModel.loginWithGoogle(idToken).observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                    goToMain();
                    break;
                case ERROR:
                    setLoading(false);
                    showErrorBanner(resource.getMessage());
                    break;
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.btnGoogleRegister.setEnabled(!loading);
        binding.btnEmailRegister.setEnabled(!loading);
        binding.pbRegisterLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showErrorBanner(String message) {
        binding.tvErrorBanner.setText(message != null ? message : "Đã có lỗi xảy ra.");
        binding.tvErrorBanner.setVisibility(View.VISIBLE);
    }

    private void hideErrorBanner() {
        binding.tvErrorBanner.setVisibility(View.GONE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
