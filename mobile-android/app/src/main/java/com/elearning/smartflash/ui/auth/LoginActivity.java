package com.elearning.smartflash.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.databinding.ActivityLoginBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.Resource;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Quizlet-style login screen supporting Email/Username + Password,
 * Google Sign-In, Forgot Password OTP flow, and Guest browsing.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
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
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (tokenManager.isLoggedIn()) {
            goToMain();
            return;
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        binding.tabGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        AuthInputHelper.bindStateStyling(binding.flIdentifierBox, binding.tilIdentifier, binding.etIdentifier, binding.tvIdentifierError);
        AuthInputHelper.bindStateStyling(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AuthInputHelper.clearError(binding.flIdentifierBox, binding.tilIdentifier, binding.etIdentifier, binding.tvIdentifierError);
                AuthInputHelper.clearError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError);
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        binding.etIdentifier.addTextChangedListener(watcher);
        binding.etPassword.addTextChangedListener(watcher);
        updateLoginButtonState();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL, getTrimmedText(binding.etIdentifier));
            startActivity(intent);
        });
    }

    private void updateLoginButtonState() {
        boolean ready = !TextUtils.isEmpty(getTrimmedText(binding.etIdentifier))
                && binding.etPassword.getText() != null
                && !TextUtils.isEmpty(binding.etPassword.getText().toString());
        if (ready) {
            binding.btnLogin.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_blue))
            );
            binding.btnLogin.setTextColor(Color.WHITE);
        } else {
            binding.btnLogin.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#F4F4F9"))
            );
            binding.btnLogin.setTextColor(Color.parseColor("#C8CEDE"));
        }
    }

    private void attemptLogin() {
        hideErrorBanner();
        AuthInputHelper.clearError(binding.flIdentifierBox, binding.tilIdentifier, binding.etIdentifier, binding.tvIdentifierError);
        AuthInputHelper.clearError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError);

        String identifier = getTrimmedText(binding.etIdentifier);
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString()
                : "";

        if (TextUtils.isEmpty(identifier)) {
            AuthInputHelper.showError(binding.flIdentifierBox, binding.tilIdentifier, binding.etIdentifier, binding.tvIdentifierError,
                    "Vui lòng nhập Email hoặc tên đăng nhập.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            AuthInputHelper.showError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError,
                    "Vui lòng nhập mật khẩu.");
            return;
        }
        if (password.length() < 6) {
            AuthInputHelper.showError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError,
                    getString(R.string.error_password_min));
            return;
        }

        authViewModel.login(identifier, password).observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
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

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        TextView tvDesc = dialogView.findViewById(R.id.tv_forgot_desc);
        TextView tvStatus = dialogView.findViewById(R.id.tv_forgot_status);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_forgot_email);
        LinearLayout llOtpFields = dialogView.findViewById(R.id.ll_otp_fields);
        TextInputEditText etOtp = dialogView.findViewById(R.id.et_otp_code);
        TextInputEditText etNewPwd = dialogView.findViewById(R.id.et_reset_new_password);

        String prefill = getTrimmedText(binding.etIdentifier);
        if (Patterns.EMAIL_ADDRESS.matcher(prefill).matches()) {
            etEmail.setText(prefill);
        }

        final boolean[] isStepReset = {false};

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_title)
                .setView(dialogView)
                .setNegativeButton("Đóng", (d, which) -> d.dismiss())
                .setPositiveButton(R.string.action_send_otp, null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                String email = getTrimmedText(etEmail);
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showDialogStatus(tvStatus, "Vui lòng nhập email hợp lệ.", true);
                    return;
                }

                if (!isStepReset[0]) {
                    positiveBtn.setEnabled(false);
                    authViewModel.forgotPassword(email).observe(this, res -> {
                        if (res == null || res.getStatus() == Resource.Status.LOADING) return;
                        positiveBtn.setEnabled(true);
                        if (res.getStatus() == Resource.Status.SUCCESS) {
                            isStepReset[0] = true;
                            etEmail.setEnabled(false);
                            llOtpFields.setVisibility(View.VISIBLE);
                            tvDesc.setText(R.string.forgot_step2_desc);
                            positiveBtn.setText(R.string.action_reset_password);
                            showDialogStatus(tvStatus, res.getData(), false);
                        } else {
                            showDialogStatus(tvStatus, res.getMessage(), true);
                        }
                    });
                } else {
                    String otp = getTrimmedText(etOtp);
                    String newPwd = etNewPwd.getText() != null ? etNewPwd.getText().toString() : "";
                    if (otp.length() < 6) {
                        showDialogStatus(tvStatus, "Mã OTP gồm 6 chữ số.", true);
                        return;
                    }
                    if (newPwd.length() < 6) {
                        showDialogStatus(tvStatus, "Mật khẩu mới phải từ 6 ký tự trở lên.", true);
                        return;
                    }
                    positiveBtn.setEnabled(false);
                    authViewModel.resetPassword(email, otp, newPwd).observe(this, res -> {
                        if (res == null || res.getStatus() == Resource.Status.LOADING) return;
                        positiveBtn.setEnabled(true);
                        if (res.getStatus() == Resource.Status.SUCCESS) {
                            Toast.makeText(this, res.getData(), Toast.LENGTH_LONG).show();
                            binding.etIdentifier.setText(email);
                            binding.etPassword.setText("");
                            dialog.dismiss();
                        } else {
                            showDialogStatus(tvStatus, res.getMessage(), true);
                        }
                    });
                }
            });
        });

        dialog.show();
    }

    private void showDialogStatus(TextView tvStatus, String msg, boolean isError) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(ContextCompat.getColor(
                this,
                isError ? R.color.quizlet_red_light : R.color.quizlet_green_light
        ));
        tvStatus.setTextColor(ContextCompat.getColor(
                this,
                isError ? R.color.quizlet_red : R.color.quizlet_green
        ));
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleLogin.setEnabled(!loading);
        binding.pbLoginLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
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
