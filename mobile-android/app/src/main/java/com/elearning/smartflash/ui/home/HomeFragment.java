package com.elearning.smartflash.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.databinding.FragmentHomeBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.ProfileUtils;

/**
 * Home tab welcoming the user and routing quickly to Search and Profile.
 * Will be populated with recent/suggested flashcard sets in T3.10.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        renderGreeting();

        binding.btnHomeExplore.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_search);
            }
        });

        binding.cardHomeProfileShortcut.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_profile);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            renderGreeting();
        }
    }

    private void renderGreeting() {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        UserSummaryDto user = tokenManager.getUser();
        if (tokenManager.isLoggedIn() && user != null) {
            String displayName = !TextUtils.isEmpty(user.getFullName())
                    ? user.getFullName()
                    : user.getUsername();
            binding.tvHomeRolePill.setText(ProfileUtils.getRoleLabel(user.getRole()));
            binding.tvHomeGreeting.setText("Xin chào, " + displayName + "!");
            binding.tvHomeSubtitle.setText("@" + user.getUsername() + " • Sẵn sàng cho phiên ôn tập FSRS hôm nay.");
        } else {
            binding.tvHomeRolePill.setText("Chế độ Khách");
            binding.tvHomeGreeting.setText("Chào mừng đến với SmartFlash!");
            binding.tvHomeSubtitle.setText("Bạn có thể khám phá nội dung công khai hoặc đăng nhập ở tab Cá nhân.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
