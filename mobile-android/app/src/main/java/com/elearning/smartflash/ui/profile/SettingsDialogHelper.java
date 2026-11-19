package com.elearning.smartflash.ui.profile;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.util.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Full-screen Quizlet-style Settings modal launched from "Cài đặt của bạn" in ProfileFragment.
 */
public final class SettingsDialogHelper {

    public interface Callback {
        void onEditProfile();

        void onChangePassword();

        void onPrivacySettings();

        void onLogout();
    }

    private SettingsDialogHelper() {
    }

    public static void show(
            Context context,
            MyProfileDto profile,
            UserSummaryDto cachedUser,
            Callback callback
    ) {
        Dialog dialog = new Dialog(context, R.style.Theme_SmartFlash);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_settings, null);
        dialog.setContentView(view);

        ThemeManager.applyStatusBarColor(dialog.getWindow(), context, R.color.sf_bg_screen);

        ImageButton btnBack = view.findViewById(R.id.btn_settings_back);
        TextView tvUsername = view.findViewById(R.id.tv_settings_username);
        TextView tvEmail = view.findViewById(R.id.tv_settings_email);
        TextView tvPasswordLabel = view.findViewById(R.id.tv_settings_password_label);

        String username = profile != null ? profile.getUsername()
                : (cachedUser != null ? cachedUser.getUsername() : "");
        String email = profile != null ? profile.getEmail()
                : (cachedUser != null ? cachedUser.getEmail() : "");
        boolean hasPassword = profile == null || profile.isHasPassword();

        tvUsername.setText(!TextUtils.isEmpty(username) ? username : "người dùng");
        tvEmail.setText(!TextUtils.isEmpty(email) ? email : "Chưa có email");
        tvPasswordLabel.setText(hasPassword
                ? R.string.profile_change_password_title
                : R.string.profile_set_password_title);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.row_settings_username).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onEditProfile();
        });

        view.findViewById(R.id.row_settings_email).setOnClickListener(v ->
                Toast.makeText(context, "Email tài khoản: " + tvEmail.getText(), Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.row_settings_password).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onChangePassword();
        });

        view.findViewById(R.id.row_settings_privacy).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onPrivacySettings();
        });

        TextView tvThemeVal = view.findViewById(R.id.tv_settings_theme_value);
        tvThemeVal.setText(ThemeManager.getThemeLabel(context));

        view.findViewById(R.id.row_settings_theme).setOnClickListener(v -> {
            String[] themes = {"Mặc định hệ thống", "Sáng", "Tối"};
            int currentMode = ThemeManager.getThemeMode(context);
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Chọn giao diện")
                    .setSingleChoiceItems(themes, currentMode, (d, which) -> {
                        tvThemeVal.setText(themes[which]);
                        d.dismiss();
                        dialog.dismiss();
                        ThemeManager.setThemeMode(context, which);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        view.findViewById(R.id.row_settings_privacy_policy).setOnClickListener(v ->
                Toast.makeText(context, "Chính sách bảo mật SmartFlash", Toast.LENGTH_SHORT).show()
        );
        view.findViewById(R.id.row_settings_terms).setOnClickListener(v ->
                Toast.makeText(context, "Điều khoản dịch vụ SmartFlash", Toast.LENGTH_SHORT).show()
        );
        view.findViewById(R.id.row_settings_help).setOnClickListener(v ->
                Toast.makeText(context, "Trung tâm trợ giúp SmartFlash", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.btn_settings_logout).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onLogout();
        });

        view.findViewById(R.id.btn_settings_delete_account).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Xóa tài khoản?")
                        .setMessage("Hành động này sẽ xóa vĩnh viễn dữ liệu học tập và không thể hoàn tác.")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xác nhận", (d, which) ->
                                Toast.makeText(context, "Vui lòng liên hệ quản trị viên để xóa tài khoản.", Toast.LENGTH_SHORT).show()
                        )
                        .show()
        );

        dialog.show();
    }
}
