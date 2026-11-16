package com.elearning.smartflash.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.databinding.FragmentLibraryBinding;
import com.elearning.smartflash.ui.auth.LoginActivity;
import com.google.android.material.tabs.TabLayout;

/**
 * Library tab organizing Sets, Folders, and Classes.
 * Full RecyclerView adapters will be connected in T3.10.
 */
public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        boolean loggedIn = tokenManager.isLoggedIn();
        binding.btnLibraryLogin.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        binding.btnLibraryLogin.setOnClickListener(v -> {
            tokenManager.setGuestMode(false);
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });

        binding.tabLibrarySections.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderSection(tab.getPosition(), loggedIn);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                renderSection(tab.getPosition(), loggedIn);
            }
        });

        renderSection(0, loggedIn);
    }

    private void renderSection(int position, boolean loggedIn) {
        if (!loggedIn) {
            binding.tvLibrarySectionTitle.setText("Đăng nhập để quản lý thư viện");
            binding.tvLibrarySectionDesc.setText("Bạn đang ở chế độ Khách. Đăng nhập để tạo bộ thẻ, sắp xếp thư mục và tham gia lớp học.");
            return;
        }
        switch (position) {
            case 1:
                binding.tvLibrarySectionTitle.setText("Thư mục học tập");
                binding.tvLibrarySectionDesc.setText("Danh sách thư mục gom nhóm các bộ thẻ của bạn sẽ hiển thị tại đây ở Task T3.10.");
                break;
            case 2:
                binding.tvLibrarySectionTitle.setText("Lớp học của bạn");
                binding.tvLibrarySectionDesc.setText("Các lớp học bạn tham gia hoặc giảng dạy sẽ đồng bộ tại đây ở Phase 7 (T7.7).");
                break;
            default:
                binding.tvLibrarySectionTitle.setText("Bộ thẻ của bạn");
                binding.tvLibrarySectionDesc.setText("Danh sách bộ thẻ đã tạo, bộ thẻ đã lưu và bộ thẻ học gần đây sẽ hiển thị tại đây ở Task T3.10.");
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
