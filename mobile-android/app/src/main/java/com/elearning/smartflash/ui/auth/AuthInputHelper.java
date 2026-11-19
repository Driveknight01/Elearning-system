package com.elearning.smartflash.ui.auth;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.view.WindowCompat;

import com.elearning.smartflash.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Shared styling helper for Auth screens: white status bar with dark icons
 * and Quizlet-like normal (#F4F4F9), focused (#FFFFFF + light-blue border),
 * and error (#FFF9C4 + red border) input states.
 */
final class AuthInputHelper {

    private static final int COLOR_BG_NORMAL = Color.parseColor("#F4F4F9");
    private static final int COLOR_BG_FOCUSED = Color.WHITE;
    private static final int COLOR_BG_ERROR = Color.parseColor("#FFF9C4");

    private AuthInputHelper() {
    }

    static void applyWhiteStatusBar(Activity activity) {
        Window window = activity.getWindow();
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.WHITE);
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    static void bindStateStyling(
            View boxContainer,
            TextInputLayout layout,
            TextInputEditText editText,
            TextView errorView
    ) {
        applyState(boxContainer, layout, editText.hasFocus(), false);
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            boolean hasError = errorView != null && errorView.getVisibility() == View.VISIBLE;
            applyState(boxContainer, layout, hasFocus, hasError);
        });
    }

    static void showError(
            View boxContainer,
            TextInputLayout layout,
            TextInputEditText editText,
            TextView errorView,
            String message
    ) {
        if (errorView != null) {
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        }
        applyState(boxContainer, layout, editText.hasFocus(), true);
    }

    static void clearError(
            View boxContainer,
            TextInputLayout layout,
            TextInputEditText editText,
            TextView errorView
    ) {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
        applyState(boxContainer, layout, editText.hasFocus(), false);
    }

    private static void applyState(
            View boxContainer,
            TextInputLayout layout,
            boolean focused,
            boolean hasError
    ) {
        if (hasError) {
            boxContainer.setBackgroundResource(R.drawable.bg_quizlet_input_error);
            layout.setBoxBackgroundColor(COLOR_BG_ERROR);
        } else if (focused) {
            boxContainer.setBackgroundResource(R.drawable.bg_quizlet_input_focused);
            layout.setBoxBackgroundColor(COLOR_BG_FOCUSED);
        } else {
            boxContainer.setBackgroundResource(R.drawable.bg_quizlet_input_normal);
            layout.setBoxBackgroundColor(COLOR_BG_NORMAL);
        }
    }
}
