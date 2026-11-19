package com.elearning.smartflash.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivityCreateAccountBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.Resource;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quizlet-style email registration details screen
 * supporting role selection (USER / TEACHER), live email-exists validation,
 * email format & password length checks, and account creation.
 */
public class CreateAccountActivity extends AppCompatActivity {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");
    private static final long EMAIL_CHECK_DEBOUNCE_MS = 450L;

    private ActivityCreateAccountBinding binding;
    private AuthViewModel authViewModel;
    private String selectedRole = "USER";

    private final Handler emailCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingEmailCheck;
    private boolean emailAlreadyExists = false;
    private String lastCheckedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AuthInputHelper.applyWhiteStatusBar(this);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        updateRoleCards();
        updateCreateButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingEmailCheck != null) {
            emailCheckHandler.removeCallbacks(pendingEmailCheck);
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.cardRoleUser.setOnClickListener(v -> {
            selectedRole = "USER";
            updateRoleCards();
        });

        binding.cardRoleTeacher.setOnClickListener(v -> {
            selectedRole = "TEACHER";
            updateRoleCards();
        });

        bindFocusValidation(binding.flFullNameBox, binding.tilFullName, binding.etFullName, binding.tvFullNameError, null);
        bindFocusValidation(binding.flUsernameBox, binding.tilUsername, binding.etUsername, binding.tvUsernameError, this::validateUsernameField);
        bindFocusValidation(binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError, () -> validateEmailField(true));
        bindFocusValidation(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError, this::validatePasswordField);

        TextWatcher generalWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCreateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFullName.addTextChangedListener(new SimpleClearWatcher(
                binding.flFullNameBox, binding.tilFullName, binding.etFullName, binding.tvFullNameError, generalWatcher));
        binding.etUsername.addTextChangedListener(new SimpleClearWatcher(
                binding.flUsernameBox, binding.tilUsername, binding.etUsername, binding.tvUsernameError, generalWatcher));
        binding.etPassword.addTextChangedListener(new SimpleClearWatcher(
                binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError, generalWatcher));

        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailAlreadyExists = false;
                lastCheckedEmail = "";
                AuthInputHelper.clearError(binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError);
                scheduleEmailCheck();
                updateCreateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnCreateAccount.setOnClickListener(v -> attemptRegister());
    }

    private void bindFocusValidation(
            View boxContainer,
            TextInputLayout layout,
            TextInputEditText editText,
            TextView errorView,
            Runnable onBlurValidator
    ) {
        AuthInputHelper.bindStateStyling(boxContainer, layout, editText, errorView);
        View.OnFocusChangeListener existing = editText.getOnFocusChangeListener();
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (existing != null) {
                existing.onFocusChange(v, hasFocus);
            }
            if (!hasFocus && onBlurValidator != null) {
                onBlurValidator.run();
            }
        });
    }

    private void scheduleEmailCheck() {
        if (pendingEmailCheck != null) {
            emailCheckHandler.removeCallbacks(pendingEmailCheck);
        }
        String email = getTrimmedText(binding.etEmail);
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return;
        }
        pendingEmailCheck = () -> checkEmailAvailability(email);
        emailCheckHandler.postDelayed(pendingEmailCheck, EMAIL_CHECK_DEBOUNCE_MS);
    }

    private void checkEmailAvailability(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(lastCheckedEmail)) {
            return;
        }
        authViewModel.checkEmailExists(normalized).observe(this, res -> {
            if (res == null || res.getStatus() == Resource.Status.LOADING) {
                return;
            }
            String current = getTrimmedText(binding.etEmail).toLowerCase(Locale.ROOT);
            if (!normalized.equals(current)) {
                return;
            }
            if (res.getStatus() == Resource.Status.SUCCESS) {
                lastCheckedEmail = normalized;
                emailAlreadyExists = Boolean.TRUE.equals(res.getData());
                if (emailAlreadyExists) {
                    AuthInputHelper.showError(
                            binding.flEmailBox,
                            binding.tilEmail,
                            binding.etEmail,
                            binding.tvEmailError,
                            getString(R.string.error_email_exists)
                    );
                } else {
                    AuthInputHelper.clearError(binding.flEmailBox, binding.tilEmail, binding.etEmail, binding.tvEmailError);
                }
                updateCreateButtonState();
            }
        });
    }

    private boolean validateUsernameField() {
        String username = getTrimmedText(binding.etUsername);
        if (TextUtils.isEmpty(username)) {
            return false;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            AuthInputHelper.showError(
                    binding.flUsernameBox,
                    binding.tilUsername,
                    binding.etUsername,
                    binding.tvUsernameError,
                    "Tên người dùng 3–50 ký tự (chữ, số, ., _, -)."
            );
            return false;
        }
        AuthInputHelper.clearError(binding.flUsernameBox, binding.tilUsername, binding.etUsername, binding.tvUsernameError);
        return true;
    }

    private boolean validateEmailField(boolean allowEmptyOnBlur) {
        String email = getTrimmedText(binding.etEmail);
        if (TextUtils.isEmpty(email)) {
            if (!allowEmptyOnBlur) {
                AuthInputHelper.showError(
                        binding.flEmailBox,
                        binding.tilEmail,
                        binding.etEmail,
                        binding.tvEmailError,
                        "Email không được để trống."
                );
            }
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            AuthInputHelper.showError(
                    binding.flEmailBox,
                    binding.tilEmail,
                    binding.etEmail,
                    binding.tvEmailError,
                    getString(R.string.error_email_invalid)
            );
            return false;
        }
        if (emailAlreadyExists) {
            AuthInputHelper.showError(
                    binding.flEmailBox,
                    binding.tilEmail,
                    binding.etEmail,
                    binding.tvEmailError,
                    getString(R.string.error_email_exists)
            );
            return false;
        }
        checkEmailAvailability(email);
        return true;
    }

    private boolean validatePasswordField() {
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString()
                : "";
        if (TextUtils.isEmpty(password)) {
            return false;
        }
        if (password.length() < 6) {
            AuthInputHelper.showError(
                    binding.flPasswordBox,
                    binding.tilPassword,
                    binding.etPassword,
                    binding.tvPasswordError,
                    getString(R.string.error_password_min)
            );
            return false;
        }
        AuthInputHelper.clearError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError);
        return true;
    }

    private void updateRoleCards() {
        boolean isLearner = "USER".equals(selectedRole);
        int activeTextColor = Color.parseColor("#282E3E");
        int mutedTextColor = ContextCompat.getColor(this, R.color.quizlet_text_muted);

        if (isLearner) {
            binding.cardRoleUser.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.cardRoleUser.setTextColor(activeTextColor);
            binding.cardRoleTeacher.setBackgroundColor(Color.TRANSPARENT);
            binding.cardRoleTeacher.setTextColor(mutedTextColor);
        } else {
            binding.cardRoleTeacher.setBackgroundResource(R.drawable.bg_segmented_active);
            binding.cardRoleTeacher.setTextColor(activeTextColor);
            binding.cardRoleUser.setBackgroundColor(Color.TRANSPARENT);
            binding.cardRoleUser.setTextColor(mutedTextColor);
        }
    }

    private void updateCreateButtonState() {
        String email = getTrimmedText(binding.etEmail);
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString()
                : "";
        boolean ready = !TextUtils.isEmpty(getTrimmedText(binding.etFullName))
                && !TextUtils.isEmpty(getTrimmedText(binding.etUsername))
                && Patterns.EMAIL_ADDRESS.matcher(email).matches()
                && !emailAlreadyExists
                && password.length() >= 6;
        if (ready) {
            binding.btnCreateAccount.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_blue))
            );
            binding.btnCreateAccount.setTextColor(Color.WHITE);
        } else {
            binding.btnCreateAccount.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#F4F4F9"))
            );
            binding.btnCreateAccount.setTextColor(Color.parseColor("#C8CEDE"));
        }
    }

    private void attemptRegister() {
        hideErrorBanner();

        String fullName = getTrimmedText(binding.etFullName);
        String username = getTrimmedText(binding.etUsername);
        String email = getTrimmedText(binding.etEmail);
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString()
                : "";

        if (TextUtils.isEmpty(fullName)) {
            AuthInputHelper.showError(binding.flFullNameBox, binding.tilFullName, binding.etFullName, binding.tvFullNameError,
                    "Họ và tên không được để trống.");
            return;
        }
        if (TextUtils.isEmpty(username) || !validateUsernameField()) {
            if (TextUtils.isEmpty(username)) {
                AuthInputHelper.showError(binding.flUsernameBox, binding.tilUsername, binding.etUsername, binding.tvUsernameError,
                        "Tên người dùng không được để trống.");
            }
            return;
        }
        if (!validateEmailField(false)) {
            return;
        }
        if (password.length() < 6) {
            AuthInputHelper.showError(binding.flPasswordBox, binding.tilPassword, binding.etPassword, binding.tvPasswordError,
                    getString(R.string.error_password_min));
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
                            String msg = resource.getMessage();
                            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("email")) {
                                emailAlreadyExists = true;
                                AuthInputHelper.showError(
                                        binding.flEmailBox,
                                        binding.tilEmail,
                                        binding.etEmail,
                                        binding.tvEmailError,
                                        getString(R.string.error_email_exists)
                                );
                                updateCreateButtonState();
                            } else {
                                showErrorBanner(msg);
                            }
                            break;
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.btnCreateAccount.setEnabled(!loading);
        binding.pbCreateLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
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

    private static final class SimpleClearWatcher implements TextWatcher {
        private final View boxContainer;
        private final TextInputLayout layout;
        private final TextInputEditText editText;
        private final TextView errorView;
        private final TextWatcher delegate;

        SimpleClearWatcher(View boxContainer, TextInputLayout layout, TextInputEditText editText, TextView errorView, TextWatcher delegate) {
            this.boxContainer = boxContainer;
            this.layout = layout;
            this.editText = editText;
            this.errorView = errorView;
            this.delegate = delegate;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            delegate.beforeTextChanged(s, start, count, after);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            AuthInputHelper.clearError(boxContainer, layout, editText, errorView);
            delegate.onTextChanged(s, start, before, count);
        }

        @Override
        public void afterTextChanged(Editable s) {
            delegate.afterTextChanged(s);
        }
    }
}
