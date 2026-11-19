package com.elearning.smartflash.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.FragmentAiChatPlaceholderBinding;
import com.elearning.smartflash.ui.main.MainActivity;

/**
 * Placeholder tab for Q-Chat / AI Chatbot (Phase 4 T4.4).
 */
public class AiChatPlaceholderFragment extends Fragment {

    private FragmentAiChatPlaceholderBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAiChatPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnAiGoHome.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_home);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
