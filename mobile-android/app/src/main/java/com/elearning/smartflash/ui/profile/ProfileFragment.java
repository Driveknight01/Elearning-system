package com.elearning.smartflash.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.databinding.FragmentProfileBinding;
import com.elearning.smartflash.ui.auth.GoogleSignInHelper;
import com.elearning.smartflash.ui.auth.LoginActivity;
import com.elearning.smartflash.ui.auth.RegisterActivity;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.ProfileUtils;
import com.elearning.smartflash.util.Resource;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile tab displaying user identity, streaks, avatar upload,
 * personal info editing, password management, and granular tab privacy controls.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private TokenManager tokenManager;
    private MyProfileDto currentProfile;

    private final ActivityResultLauncher<String> avatarPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !isAdded()) {
                    return;
                }
                viewModel.uploadAvatar(uri).observe(getViewLifecycleOwner(), res -> {
                    if (res == null) {
                        return;
                    }
                    switch (res.getStatus()) {
                        case LOADING:
                            binding.pbProfileLoading.setVisibility(View.VISIBLE);
                            break;
                        case SUCCESS:
                            binding.pbProfileLoading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(),
                                    res.getMessage() != null ? res.getMessage() : "Đã cập nhật ảnh đại diện!",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case ERROR:
                            binding.pbProfileLoading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                            break;
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tokenManager = TokenManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupListeners();
        observeProfile();
        refreshSessionView();
    }

    private void setupListeners() {
        binding.swipeProfileRefresh.setColorSchemeResources(R.color.quizlet_blue);
        binding.swipeProfileRefresh.setOnRefreshListener(() -> {
            if (tokenManager.isLoggedIn()) {
                viewModel.loadMyProfile();
            } else {
                binding.swipeProfileRefresh.setRefreshing(false);
            }
        });

        binding.btnGuestLogin.setOnClickListener(v -> {
            tokenManager.setGuestMode(false);
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });

        binding.btnGuestRegister.setOnClickListener(v -> {
            tokenManager.setGuestMode(false);
            startActivity(new Intent(requireContext(), RegisterActivity.class));
        });

        binding.btnProfileBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_home);
            }
        });

        binding.cardYourSettings.setOnClickListener(v ->
                SettingsDialogHelper.show(
                        requireContext(),
                        currentProfile,
                        tokenManager.getUser(),
                        new SettingsDialogHelper.Callback() {
                            @Override
                            public void onEditProfile() {
                                showEditProfileDialog();
                            }

                            @Override
                            public void onChangePassword() {
                                showChangePasswordDialog();
                            }

                            @Override
                            public void onPrivacySettings() {
                                showPrivacyDialog();
                            }

                            @Override
                            public void onLogout() {
                                performLogout();
                            }
                        }
                )
        );

        binding.cardActivity.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Lịch sử hoạt động & thông báo sẽ đồng bộ ở Phase 8 (T8.8).", Toast.LENGTH_SHORT).show()
        );

        binding.tvAchievementsViewAll.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Bộ sưu tập huy hiệu thành tựu sẽ mở ở Phase 8 (T8.8).", Toast.LENGTH_SHORT).show()
        );

        binding.flProfileAvatarWrap.setOnClickListener(v -> openAvatarChooser());
        binding.btnChangeAvatar.setOnClickListener(v -> openAvatarChooser());
        binding.rowPrivacySettings.setOnClickListener(v -> showPrivacyDialog());
        binding.rowEditProfile.setOnClickListener(v -> showEditProfileDialog());
        binding.rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnPreviewPublicProfile.setOnClickListener(v -> openPublicProfilePreview());
        binding.btnLogout.setOnClickListener(v -> performLogout());
    }

    private void openAvatarChooser() {
        ChooseAvatarDialogHelper.show(requireContext(), new ChooseAvatarDialogHelper.Callback() {
            @Override
            public void onPickFromGallery() {
                avatarPickerLauncher.launch("image/*");
            }

            @Override
            public void onPresetConfirmed(android.net.Uri renderedPngUri) {
                viewModel.uploadAvatar(renderedPngUri).observe(getViewLifecycleOwner(), res -> {
                    if (res == null) return;
                    switch (res.getStatus()) {
                        case LOADING:
                            binding.pbProfileLoading.setVisibility(View.VISIBLE);
                            break;
                        case SUCCESS:
                            binding.pbProfileLoading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(),
                                    res.getMessage() != null ? res.getMessage() : "Đã cập nhật ảnh đại diện!",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case ERROR:
                            binding.pbProfileLoading.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_LONG).show();
                            break;
                    }
                });
            }
        });
    }

    private void refreshSessionView() {
        if (!tokenManager.isLoggedIn()) {
            binding.cardGuestPrompt.setVisibility(View.VISIBLE);
            binding.llProfileContent.setVisibility(View.GONE);
            binding.swipeProfileRefresh.setEnabled(false);
            return;
        }

        binding.cardGuestPrompt.setVisibility(View.GONE);
        binding.llProfileContent.setVisibility(View.VISIBLE);
        binding.swipeProfileRefresh.setEnabled(true);

        UserSummaryDto cached = tokenManager.getUser();
        if (cached != null && currentProfile == null) {
            bindCachedUser(cached);
        }
        viewModel.loadMyProfile();
    }

    private void observeProfile() {
        viewModel.getProfileState().observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || binding == null) {
                return;
            }
            switch (resource.getStatus()) {
                case LOADING:
                    if (!binding.swipeProfileRefresh.isRefreshing()) {
                        binding.pbProfileLoading.setVisibility(View.VISIBLE);
                    }
                    break;
                case SUCCESS:
                    binding.pbProfileLoading.setVisibility(View.GONE);
                    binding.swipeProfileRefresh.setRefreshing(false);
                    if (resource.getData() != null) {
                        currentProfile = resource.getData();
                        bindProfile(currentProfile);
                    }
                    break;
                case ERROR:
                    binding.pbProfileLoading.setVisibility(View.GONE);
                    binding.swipeProfileRefresh.setRefreshing(false);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        });
    }

    private void bindCachedUser(UserSummaryDto user) {
        String displayName = !TextUtils.isEmpty(user.getFullName()) ? user.getFullName() : user.getUsername();
        binding.tvFullName.setText(displayName);
        binding.tvUsername.setText("@" + user.getUsername());
        binding.tvEmail.setText(user.getEmail());
        binding.tvRoleBadge.setText(ProfileUtils.getRoleLabel(user.getRole()));
        binding.ivVerifiedTeacher.setVisibility(user.isVerifiedTeacher() ? View.VISIBLE : View.GONE);
        PublicProfileDialogHelper.bindAvatar(
                requireContext(),
                user.getAvatarUrl(),
                displayName,
                user.getUsername(),
                binding.ivAvatar,
                binding.tvAvatarInitials
        );
    }

    private void bindProfile(MyProfileDto profile) {
        String displayName = !TextUtils.isEmpty(profile.getFullName())
                ? profile.getFullName()
                : profile.getUsername();

        binding.tvFullName.setText(displayName);
        binding.tvUsername.setText("@" + profile.getUsername());
        binding.tvEmail.setText(profile.getEmail());
        binding.tvBio.setText(!TextUtils.isEmpty(profile.getBio())
                ? profile.getBio()
                : "Chưa có giới thiệu bản thân.");
        binding.tvRoleBadge.setText(ProfileUtils.getRoleLabel(profile.getRole()));
        binding.ivVerifiedTeacher.setVisibility(profile.isVerifiedTeacher() ? View.VISIBLE : View.GONE);

        String visibilityLabel = ProfileUtils.getVisibilityLabel(profile.getProfileVisibility());
        binding.tvVisibilityBadge.setText(visibilityLabel);
        binding.tvRowPrivacyDesc.setText("Hiện tại: " + visibilityLabel + " • Bật/tắt 4 tab");

        PublicProfileDialogHelper.bindAvatar(
                requireContext(),
                profile.getAvatarUrl(),
                displayName,
                profile.getUsername(),
                binding.ivAvatar,
                binding.tvAvatarInitials
        );

        int currentStreak = profile.getCurrentStreak() != null ? profile.getCurrentStreak() : 0;
        int longestStreak = profile.getLongestStreak() != null ? profile.getLongestStreak() : 0;
        long totalXp = profile.getTotalXp() != null ? profile.getTotalXp() : 0L;
        binding.tvCurrentStreak.setText(String.valueOf(currentStreak));
        binding.tvLongestStreak.setText(String.valueOf(longestStreak));
        binding.tvTotalXp.setText(String.valueOf(totalXp));

        int goal = profile.getDailyGoalCards() != null ? profile.getDailyGoalCards() : 20;
        String tz = !TextUtils.isEmpty(profile.getTimezone()) ? profile.getTimezone() : "Asia/Ho_Chi_Minh";
        binding.tvGoalTimezone.setText("Mục tiêu: " + goal + " thẻ/ngày • " + tz);

        binding.tvPrivacyTabsSummary.setText(formatPrivacyTabSummary(profile.getProfilePrivacyConfig()));

        if (profile.isHasPassword()) {
            binding.tvRowPasswordTitle.setText(R.string.profile_change_password_title);
            binding.tvRowPasswordDesc.setText("Cập nhật mật khẩu đăng nhập tài khoản");
        } else {
            binding.tvRowPasswordTitle.setText(R.string.profile_set_password_title);
            binding.tvRowPasswordDesc.setText("Tài khoản Google chưa có mật khẩu riêng");
        }
    }

    private String formatPrivacyTabSummary(ProfilePrivacyConfigDto config) {
        if (config == null) {
            return "Tab công khai: Bộ thẻ, Thư mục, Lớp học, Thành tựu";
        }
        List<String> visibleNames = new ArrayList<>();
        if (config.isShowSets()) visibleNames.add("Bộ thẻ");
        if (config.isShowFolders()) visibleNames.add("Thư mục");
        if (config.isShowClasses()) visibleNames.add("Lớp học");
        if (config.isShowAchievements()) visibleNames.add("Thành tựu");
        if (visibleNames.isEmpty()) {
            return "Tab công khai: Đang ẩn toàn bộ 4 tab với người khác";
        }
        return "Tab công khai: " + TextUtils.join(", ", visibleNames);
    }

    private void showPrivacyDialog() {
        if (currentProfile == null) {
            Toast.makeText(requireContext(), "Đang tải hồ sơ, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_privacy, null);
        TextView tvStatus = dialogView.findViewById(R.id.tv_privacy_status);
        LinearLayout cardPublic = dialogView.findViewById(R.id.card_vis_public);
        LinearLayout cardLoggedIn = dialogView.findViewById(R.id.card_vis_logged_in);
        LinearLayout cardPrivate = dialogView.findViewById(R.id.card_vis_private);
        RadioButton rbPublic = dialogView.findViewById(R.id.rb_vis_public);
        RadioButton rbLoggedIn = dialogView.findViewById(R.id.rb_vis_logged_in);
        RadioButton rbPrivate = dialogView.findViewById(R.id.rb_vis_private);

        MaterialSwitch swSets = dialogView.findViewById(R.id.switch_show_sets);
        MaterialSwitch swFolders = dialogView.findViewById(R.id.switch_show_folders);
        MaterialSwitch swClasses = dialogView.findViewById(R.id.switch_show_classes);
        MaterialSwitch swAchievements = dialogView.findViewById(R.id.switch_show_achievements);

        ProfilePrivacyConfigDto existingConfig = currentProfile.getProfilePrivacyConfig();
        if (existingConfig != null) {
            swSets.setChecked(existingConfig.isShowSets());
            swFolders.setChecked(existingConfig.isShowFolders());
            swClasses.setChecked(existingConfig.isShowClasses());
            swAchievements.setChecked(existingConfig.isShowAchievements());
        }

        final String[] selectedVisibility = {
                currentProfile.getProfileVisibility() != null
                        ? currentProfile.getProfileVisibility()
                        : "PUBLIC"
        };

        Runnable renderSelection = () -> {
            boolean isPublic = "PUBLIC".equals(selectedVisibility[0]);
            boolean isLoggedIn = "LOGGED_IN_ONLY".equals(selectedVisibility[0]);
            boolean isPrivate = "PRIVATE".equals(selectedVisibility[0]);

            cardPublic.setBackgroundResource(isPublic ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected);
            cardLoggedIn.setBackgroundResource(isLoggedIn ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected);
            cardPrivate.setBackgroundResource(isPrivate ? R.drawable.bg_role_card_selected : R.drawable.bg_role_card_unselected);

            rbPublic.setChecked(isPublic);
            rbLoggedIn.setChecked(isLoggedIn);
            rbPrivate.setChecked(isPrivate);
        };
        renderSelection.run();

        cardPublic.setOnClickListener(v -> {
            selectedVisibility[0] = "PUBLIC";
            renderSelection.run();
        });
        cardLoggedIn.setOnClickListener(v -> {
            selectedVisibility[0] = "LOGGED_IN_ONLY";
            renderSelection.run();
        });
        cardPrivate.setOnClickListener(v -> {
            selectedVisibility[0] = "PRIVATE";
            renderSelection.run();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_privacy_title)
                .setView(dialogView)
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .setPositiveButton("Lưu cài đặt", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                ProfilePrivacyConfigDto newConfig = new ProfilePrivacyConfigDto(
                        swSets.isChecked(),
                        swFolders.isChecked(),
                        swClasses.isChecked(),
                        swAchievements.isChecked()
                );
                positiveBtn.setEnabled(false);
                viewModel.updatePrivacy(selectedVisibility[0], newConfig)
                        .observe(getViewLifecycleOwner(), res -> {
                            if (res == null || res.getStatus() == Resource.Status.LOADING) {
                                return;
                            }
                            positiveBtn.setEnabled(true);
                            if (res.getStatus() == Resource.Status.SUCCESS) {
                                Toast.makeText(requireContext(),
                                        res.getMessage() != null ? res.getMessage() : "Đã lưu quyền riêng tư!",
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                showDialogStatus(tvStatus, res.getMessage());
                            }
                        });
            });
        });

        dialog.show();
    }

    private void showEditProfileDialog() {
        if (currentProfile == null) {
            Toast.makeText(requireContext(), "Đang tải hồ sơ, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextView tvStatus = dialogView.findViewById(R.id.tv_edit_status);
        TextInputLayout tilFullName = dialogView.findViewById(R.id.til_edit_full_name);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.til_edit_username);
        TextInputLayout tilDailyGoal = dialogView.findViewById(R.id.til_edit_daily_goal);
        TextInputEditText etFullName = dialogView.findViewById(R.id.et_edit_full_name);
        TextInputEditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        TextInputEditText etBio = dialogView.findViewById(R.id.et_edit_bio);
        TextInputEditText etDailyGoal = dialogView.findViewById(R.id.et_edit_daily_goal);
        TextInputEditText etTimezone = dialogView.findViewById(R.id.et_edit_timezone);

        etFullName.setText(currentProfile.getFullName());
        etUsername.setText(currentProfile.getUsername());
        etBio.setText(currentProfile.getBio());
        etDailyGoal.setText(String.valueOf(
                currentProfile.getDailyGoalCards() != null ? currentProfile.getDailyGoalCards() : 20
        ));
        etTimezone.setText(!TextUtils.isEmpty(currentProfile.getTimezone())
                ? currentProfile.getTimezone()
                : "Asia/Ho_Chi_Minh");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_edit_title)
                .setView(dialogView)
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .setPositiveButton("Lưu thay đổi", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                tilFullName.setError(null);
                tilUsername.setError(null);
                tilDailyGoal.setError(null);
                tvStatus.setVisibility(View.GONE);

                String fullName = getText(etFullName);
                String username = getText(etUsername);
                String bio = getText(etBio);
                String goalStr = getText(etDailyGoal);
                String timezone = getText(etTimezone);

                if (TextUtils.isEmpty(fullName)) {
                    tilFullName.setError("Họ và tên không được để trống");
                    return;
                }
                if (username.length() < 3) {
                    tilUsername.setError("Tên đăng nhập tối thiểu 3 ký tự");
                    return;
                }
                int dailyGoal = 20;
                if (!TextUtils.isEmpty(goalStr)) {
                    try {
                        dailyGoal = Integer.parseInt(goalStr);
                        if (dailyGoal < 1 || dailyGoal > 500) {
                            tilDailyGoal.setError("Mục tiêu từ 1 đến 500 thẻ");
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        tilDailyGoal.setError("Số thẻ không hợp lệ");
                        return;
                    }
                }

                positiveBtn.setEnabled(false);
                viewModel.updateProfile(
                        username,
                        fullName,
                        bio,
                        dailyGoal,
                        TextUtils.isEmpty(timezone) ? "Asia/Ho_Chi_Minh" : timezone
                ).observe(getViewLifecycleOwner(), res -> {
                    if (res == null || res.getStatus() == Resource.Status.LOADING) {
                        return;
                    }
                    positiveBtn.setEnabled(true);
                    if (res.getStatus() == Resource.Status.SUCCESS) {
                        Toast.makeText(requireContext(),
                                res.getMessage() != null ? res.getMessage() : "Cập nhật hồ sơ thành công!",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        showDialogStatus(tvStatus, res.getMessage());
                    }
                });
            });
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        if (currentProfile == null) {
            Toast.makeText(requireContext(), "Đang tải hồ sơ, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        TextView tvDesc = dialogView.findViewById(R.id.tv_pwd_desc);
        TextView tvStatus = dialogView.findViewById(R.id.tv_pwd_status);
        TextInputLayout tilCurrent = dialogView.findViewById(R.id.til_current_password);
        TextInputLayout tilNew = dialogView.findViewById(R.id.til_new_password);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.til_confirm_password);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNew = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.et_confirm_password);

        boolean hasPassword = currentProfile.isHasPassword();
        if (!hasPassword) {
            tilCurrent.setVisibility(View.GONE);
            tvDesc.setText("Tài khoản của bạn đăng nhập bằng Google. Đặt mật khẩu mới để có thể đăng nhập bằng Email/Username.");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(hasPassword ? R.string.profile_change_password_title : R.string.profile_set_password_title)
                .setView(dialogView)
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .setPositiveButton("Xác nhận", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> {
                tilCurrent.setError(null);
                tilNew.setError(null);
                tilConfirm.setError(null);
                tvStatus.setVisibility(View.GONE);

                String currentPwd = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
                String newPwd = etNew.getText() != null ? etNew.getText().toString() : "";
                String confirmPwd = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

                if (hasPassword && TextUtils.isEmpty(currentPwd)) {
                    tilCurrent.setError("Vui lòng nhập mật khẩu hiện tại");
                    return;
                }
                if (newPwd.length() < 6) {
                    tilNew.setError("Mật khẩu mới tối thiểu 6 ký tự");
                    return;
                }
                if (!newPwd.equals(confirmPwd)) {
                    tilConfirm.setError("Mật khẩu xác nhận không khớp");
                    return;
                }

                positiveBtn.setEnabled(false);
                viewModel.changePassword(hasPassword ? currentPwd : null, newPwd)
                        .observe(getViewLifecycleOwner(), res -> {
                            if (res == null || res.getStatus() == Resource.Status.LOADING) {
                                return;
                            }
                            positiveBtn.setEnabled(true);
                            if (res.getStatus() == Resource.Status.SUCCESS) {
                                Toast.makeText(requireContext(),
                                        res.getMessage() != null ? res.getMessage() : "Đổi mật khẩu thành công!",
                                        Toast.LENGTH_SHORT).show();
                                viewModel.loadMyProfile();
                                dialog.dismiss();
                            } else {
                                showDialogStatus(tvStatus, res.getMessage());
                            }
                        });
            });
        });

        dialog.show();
    }

    private void openPublicProfilePreview() {
        String username = currentProfile != null
                ? currentProfile.getUsername()
                : (tokenManager.getUser() != null ? tokenManager.getUser().getUsername() : null);
        if (TextUtils.isEmpty(username)) {
            return;
        }
        binding.pbProfileLoading.setVisibility(View.VISIBLE);
        viewModel.getPublicProfile(username).observe(getViewLifecycleOwner(), res -> {
            if (res == null || res.getStatus() == Resource.Status.LOADING) {
                return;
            }
            binding.pbProfileLoading.setVisibility(View.GONE);
            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                PublicProfileDialogHelper.show(requireContext(), res.getData());
            } else {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        binding.btnLogout.setEnabled(false);
        GoogleSignInHelper.signOutSilent(requireContext());
        viewModel.logout(() -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });
    }

    private void showDialogStatus(TextView tvStatus, String msg) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg != null ? msg : "Đã có lỗi xảy ra.");
        tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.quizlet_red_light));
        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.quizlet_red));
    }

    private static String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
