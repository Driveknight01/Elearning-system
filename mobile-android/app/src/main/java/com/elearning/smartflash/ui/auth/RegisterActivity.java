package com.elearning.smartflash.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivityRegisterBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

/**
 * Quizlet-style registration screen supporting role choice (USER / TEACHER)
 * and unified Google Sign-In.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private String selectedRole = "USER";

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
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        updateRoleCards();
    }

    private void setupListeners() {
        binding.tabGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.cardRoleUser.setOnClickListener(v -> {
            selectedRole = "USER";
            updateRoleCards();
        });

        binding.cardRoleTeacher.setOnClickListener(v -> {
            selectedRole = "TEACHER";
            updateRoleCards();
        });

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.btnGoogleRegister.setOnClickListener(v -> startGoogleSignIn());
    }

    private void updateRoleCards() {
        boolean isLearner = "USER".equals(selectedRole);
        binding.cardRoleUser.setBackgroundResource(
                isLearner ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected
        );
        binding.cardRoleTeacher.setBackgroundResource(
                !isLearner ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected
        );
    }

    private void attemptRegister() {
        hideErrorBanner();
        binding.tilFullName.setError(null);
        binding.tilUsername.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        String fullName = getTrimmedText(binding.etFullName);
        String username = getTrimmedText(binding.etUsername);
        String email = getTrimmedText(binding.etEmail);
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString()
                : "";

        if (TextUtils.isEmpty(fullName)) {
            binding.tilFullName.setError("Họ và tên không được để trống");
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            binding.tilUsername.setError("Username 3–50 ký tự (chữ, số, ., _, -)");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Email không đúng định dạng");
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Mật khẩu phải từ 6 ký tự trở lên");
            return;
        }

        authViewModel.register(username, email, password, fullName, selectedRole)
                .observe(this, resource -> {
                    if (resource == null) return;
                    switch (resource.getStatus()) {
                        case LOADING:
                            setLoading(true);
                            break;
                        case SUCCESS:
                            setLoading(false);
                            Toast.makeText(this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();
                            goToMain();
                            break;
                        case ERROR:
                            setLoading(false);
                            showErrorBanner(resource.getMessage());
                            break;
                    }
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
        binding.btnRegister.setEnabled(!loading);
        binding.btnGoogleRegister.setEnabled(!loading);
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

    private static String getTrimmedText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
