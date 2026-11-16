package com.elearning.smartflash.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.databinding.FragmentSearchBinding;
import com.elearning.smartflash.ui.profile.ProfileViewModel;
import com.elearning.smartflash.ui.profile.PublicProfileDialogHelper;
import com.elearning.smartflash.util.ProfileUtils;
import com.elearning.smartflash.util.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Search tab supporting {@code @username} public profile lookup
 * via {@code GET /api/v1/users/profile/{username}}.
 * Full-text public flashcard set search will be added in T3.10.
 */
public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private ProfileViewModel profileViewModel;
    private PublicProfileDto lastLoadedProfile;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        binding.btnSearchSubmit.setOnClickListener(v -> performLookup());
        binding.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performLookup();
                return true;
            }
            return false;
        });

        binding.btnOpenPublicProfileDialog.setOnClickListener(v -> {
            if (lastLoadedProfile != null) {
                PublicProfileDialogHelper.show(requireContext(), lastLoadedProfile);
            }
        });
    }

    private void performLookup() {
        String raw = binding.etSearchQuery.getText() != null
                ? binding.etSearchQuery.getText().toString().trim()
                : "";
        if (raw.startsWith("@")) {
            raw = raw.substring(1).trim();
        }

        binding.tvSearchFeedback.setVisibility(View.GONE);
        binding.cardSearchProfileResult.setVisibility(View.GONE);
        lastLoadedProfile = null;

        if (TextUtils.isEmpty(raw)) {
            binding.tilSearchQuery.setError("Vui lòng nhập username cần tìm");
            return;
        }
        binding.tilSearchQuery.setError(null);

        profileViewModel.getPublicProfile(raw).observe(getViewLifecycleOwner(), res -> {
            if (res == null) {
                return;
            }
            switch (res.getStatus()) {
                case LOADING:
                    binding.pbSearchLoading.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.pbSearchLoading.setVisibility(View.GONE);
                    if (res.getData() != null) {
                        lastLoadedProfile = res.getData();
                        bindPublicProfile(res.getData());
                    }
                    break;
                case ERROR:
                    binding.pbSearchLoading.setVisibility(View.GONE);
                    binding.tvSearchFeedback.setText(res.getMessage());
                    binding.tvSearchFeedback.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void bindPublicProfile(PublicProfileDto profile) {
        binding.cardSearchProfileResult.setVisibility(View.VISIBLE);
        String displayName = !TextUtils.isEmpty(profile.getFullName())
                ? profile.getFullName()
                : profile.getUsername();

        binding.tvResultFullName.setText(displayName);
        binding.tvResultMeta.setText("@" + profile.getUsername()
                + " • " + ProfileUtils.getRoleLabel(profile.getRole())
                + " • " + ProfileUtils.getVisibilityLabel(profile.getProfileVisibility()));

        PublicProfileDialogHelper.bindAvatar(
                requireContext(),
                profile.getAvatarUrl(),
                displayName,
                profile.getUsername(),
                binding.ivResultAvatar,
                binding.tvResultInitials
        );

        if (!TextUtils.isEmpty(profile.getBio())) {
            binding.tvResultBio.setText(profile.getBio());
            binding.tvResultBio.setVisibility(View.VISIBLE);
        } else {
            binding.tvResultBio.setVisibility(View.GONE);
        }

        ProfilePrivacyConfigDto tabs = profile.getVisibleTabs();
        List<String> openTabs = new ArrayList<>();
        if (tabs == null || tabs.isShowSets()) openTabs.add("Bộ thẻ");
        if (tabs == null || tabs.isShowFolders()) openTabs.add("Thư mục");
        if (tabs == null || tabs.isShowClasses()) openTabs.add("Lớp học");
        if (tabs == null || tabs.isShowAchievements()) openTabs.add("Thành tựu");

        binding.tvResultTabsSummary.setText(openTabs.isEmpty()
                ? "Người dùng này đã ẩn tất cả các tab."
                : "Các tab được phép xem: " + TextUtils.join(", ", openTabs));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
