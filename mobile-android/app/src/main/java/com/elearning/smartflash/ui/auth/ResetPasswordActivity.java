package com.elearning.smartflash.ui.auth;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivityResetPasswordBinding;
import com.elearning.smartflash.util.Resource;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Quizlet-style full-screen Reset Password flow:
 * Step 1 sends a 6-digit OTP to the user's email; Step 2 verifies OTP and sets a new password.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private ActivityResetPasswordBinding binding;
    private AuthViewModel authViewModel;
    private boolean isStepReset = false;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthInputHelper.applyWhiteStatusBar(this);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        String prefillEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (!TextUtils.isEmpty(prefillEmail) && Patterns.EMAIL_ADDRESS.matcher(prefillEmail.trim()).matches()) {
            binding.etEmail.setText(prefillEmail.trim());
        }

        setupInputs();
        setupListeners();
        updateSubmitButtonState();
    }

    private void setupInputs() {
        AuthInputHelper.bindStateStyling(binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError);
        AuthInputHelper.bindStateStyling(binding.flOtpBox, binding.tilOtp, binding.etOtp, binding.tvOtpError);
        AuthInputHelper.bindStateStyling(
                binding.flNewPasswordBox, binding.tilNewPassword, binding.etNewPassword, binding.tvNewPasswordError);

        binding.etEmail.addTextChangedListener(new SimpleFieldWatcher(() -> {
            AuthInputHelper.clearError(binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError);
            hideBanners();
            updateSubmitButtonState();
        }));
        binding.etOtp.addTextChangedListener(new SimpleFieldWatcher(() -> {
            AuthInputHelper.clearError(binding.flOtpBox, binding.tilOtp, binding.etOtp, binding.tvOtpError);
            hideErrorBanner();
            updateSubmitButtonState();
        }));
        binding.etNewPassword.addTextChangedListener(new SimpleFieldWatcher(() -> {
            AuthInputHelper.clearError(
                    binding.flNewPasswordBox, binding.tilNewPassword, binding.etNewPassword, binding.tvNewPasswordError);
            hideErrorBanner();
            updateSubmitButtonState();
        }));
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSubmitReset.setOnClickListener(v -> {
            if (!isStepReset) {
                requestOtp(false);
            } else {
                submitResetPassword();
            }
        });
        binding.tvResendOtp.setOnClickListener(v -> requestOtp(true));
    }

    private void requestOtp(boolean isResend) {
        hideBanners();
        String email = getText(binding.etEmail);
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            AuthInputHelper.showError(
                    binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError,
                    "Vui lòng nhập email hợp lệ.");
            return;
        }

        authViewModel.forgotPassword(email).observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    showStep2(resource.getData(), isResend);
                    break;
                case ERROR:
                    setLoading(false);
                    showErrorBanner(resource.getMessage());
                    break;
            }
        });
    }

    private void showStep2(String serverMessage, boolean isResend) {
        if (!isStepReset) {
            isStepReset = true;
            TransitionManager.beginDelayedTransition(binding.llResetRoot);
            binding.llStep2Section.setVisibility(View.VISIBLE);
            binding.etEmail.setEnabled(false);
            binding.tvResetDesc.setText(R.string.forgot_step2_desc);
            binding.btnSubmitReset.setText(R.string.action_reset_password);
            binding.etOtp.requestFocus();
        }
        String msg = !TextUtils.isEmpty(serverMessage)
                ? serverMessage
                : (isResend ? "Đã gửi lại mã OTP mới." : "Đã gửi mã OTP về email của bạn.");
        binding.tvStatusBanner.setText(msg);
        binding.tvStatusBanner.setVisibility(View.VISIBLE);
        updateSubmitButtonState();
    }

    private void submitResetPassword() {
        hideErrorBanner();
        String email = getText(binding.etEmail);
        String otp = getText(binding.etOtp);
        String newPassword = getText(binding.etNewPassword);

        if (otp.length() != 6) {
            AuthInputHelper.showError(
                    binding.flOtpBox, binding.tilOtp, binding.etOtp, binding.tvOtpError,
                    "Mã OTP phải gồm đúng 6 chữ số.");
            return;
        }
        if (newPassword.length() < 6) {
            AuthInputHelper.showError(
                    binding.flNewPasswordBox, binding.tilNewPassword, binding.etNewPassword, binding.tvNewPasswordError,
                    "Mật khẩu mới tối thiểu 6 ký tự.");
            return;
        }

        authViewModel.resetPassword(email, otp, newPassword).observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(this, "Đặt lại mật khẩu thành công! Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case ERROR:
                    setLoading(false);
                    showErrorBanner(resource.getMessage());
                    break;
            }
        });
    }

    private void updateSubmitButtonState() {
        String email = getText(binding.etEmail);
        boolean validEmail = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean ready;
        if (!isStepReset) {
            ready = validEmail;
        } else {
            ready = validEmail
                    && getText(binding.etOtp).length() == 6
                    && getText(binding.etNewPassword).length() >= 6;
        }

        boolean enabled = ready && !isLoading;
        binding.btnSubmitReset.setEnabled(enabled);
        if (enabled) {
            binding.btnSubmitReset.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_blue)));
            binding.btnSubmitReset.setTextColor(Color.WHITE);
        } else {
            binding.btnSubmitReset.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F4F4F9")));
            binding.btnSubmitReset.setTextColor(Color.parseColor("#B8C0D2"));
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        binding.pbResetLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.tvResendOtp.setEnabled(!loading);
        updateSubmitButtonState();
    }

    private void showErrorBanner(String message) {
        binding.tvErrorBanner.setText(message != null ? message : "Đã có lỗi xảy ra.");
        binding.tvErrorBanner.setVisibility(View.VISIBLE);
    }

    private void hideErrorBanner() {
        binding.tvErrorBanner.setVisibility(View.GONE);
    }

    private void hideBanners() {
        binding.tvErrorBanner.setVisibility(View.GONE);
        binding.tvStatusBanner.setVisibility(View.GONE);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private static final class SimpleFieldWatcher implements TextWatcher {
        private final Runnable onChanged;

        SimpleFieldWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChanged.run();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
