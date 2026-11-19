package com.elearning.smartflash.ui.home;

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.databinding.FragmentHomeBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.util.ProfileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Quizlet-style Newbie Home tab featuring:
 * - Top bar (Search on left, Avatar on right, no Upgrade button)
 * - Block 1: Personalize your content
 * - Block 2: Study exactly what you need (Create flashcards)
 * - Block 3: Learn questions (Interactive 5-question trivia)
 * - Block 4: Staff picks (Horizontal flashcard set recommendations)
 * - Block 5: Switch it up with a game (Quick tab switcher for Match, Blast, Blocks)
 */
public class HomeFragment extends Fragment {

    private static final class TriviaQuestion {
        final String question;
        final String[] options;
        final int correctIndex;

        TriviaQuestion(String question, String[] options, int correctIndex) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    private static final List<TriviaQuestion> TRIVIA_QUESTIONS = Arrays.asList(
            new TriviaQuestion(
                    "Bạch tuộc có bao nhiêu trái tim?",
                    new String[]{"5", "3", "7", "1"},
                    1
            ),
            new TriviaQuestion(
                    "Thuật toán FSRS-4.5 giúp tối ưu điều gì khi ôn tập?",
                    new String[]{"Độ sáng màn hình", "Khoảng thời gian lặp lại ngắt quãng", "Tốc độ tải trang", "Dung lượng ảnh"},
                    1
            ),
            new TriviaQuestion(
                    "Hành tinh nào lớn nhất trong Hệ Mặt Trời?",
                    new String[]{"Sao Hỏa", "Sao Thổ", "Sao Mộc", "Sao Kim"},
                    2
            ),
            new TriviaQuestion(
                    "Nguyên tố hóa học nào có ký hiệu là 'Au'?",
                    new String[]{"Bạc", "Nhôm", "Vàng", "Đồng"},
                    2
            ),
            new TriviaQuestion(
                    "Phương pháp Active Recall khuyến khích người học làm gì?",
                    new String[]{"Đọc đi đọc lại thụ động", "Tự kiểm tra và gợi nhớ chủ động", "Chép nguyên văn sách giáo khoa", "Chỉ học trước ngày thi"},
                    1
            )
    );

    private FragmentHomeBinding binding;
    private StaffPickAdapter staffPickAdapter;
    private int currentTriviaIndex = 0;
    private boolean triviaLocked = false;
    private int activeGameTab = 0; // 0 = Match, 1 = Blast, 2 = Blocks
    private int currentStatusBarColor = 0;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final Handler handler = new Handler(Looper.getMainLooper());

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

        renderAvatar();
        setupTopBar();
        setupScrollStatusBar();
        setupOnboardingBlocks();
        setupTriviaBlock();
        setupStaffPicksBlock();
        setupGameSwitchBlock();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            renderAvatar();
            syncStatusBarColor();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            syncStatusBarColor();
        }
    }

    public void syncStatusBarColor() {
        if (binding == null) return;
        updateStatusBarForScroll(binding.homeScrollView.getScrollY());
    }

    private void setupScrollStatusBar() {
        binding.homeScrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        updateStatusBarForScroll(scrollY)
        );
        syncStatusBarColor();
    }

    private void updateStatusBarForScroll(int scrollY) {
        if (binding == null) return;
        int gradientHeight = binding.viewHomeTopGradient.getHeight();
        if (gradientHeight <= 0) {
            gradientHeight = Math.round(220 * getResources().getDisplayMetrics().density);
        }
        float fadeDistance = Math.max(1f, gradientHeight * 0.65f);
        float fraction = Math.min(1f, Math.max(0f, scrollY / fadeDistance));
        int topColor = ContextCompat.getColor(requireContext(), R.color.sf_home_top_status);
        int screenColor = ContextCompat.getColor(requireContext(), R.color.sf_bg_screen);
        int nextColor = fraction >= 1f
                ? screenColor
                : (int) argbEvaluator.evaluate(fraction, topColor, screenColor);
        if (nextColor != currentStatusBarColor || fraction == 0f || fraction >= 1f) {
            currentStatusBarColor = nextColor;
            if (getActivity() instanceof MainActivity activity) {
                activity.updateHomeScrollStatusBar(currentStatusBarColor);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    private void renderAvatar() {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        UserSummaryDto user = tokenManager.getUser();
        if (user != null) {
            String displayName = !TextUtils.isEmpty(user.getFullName())
                    ? user.getFullName()
                    : user.getUsername();
            binding.tvHomeAvatarInitials.setText(ProfileUtils.getInitials(displayName, user.getUsername()));

            if (!TextUtils.isEmpty(user.getAvatarUrl())) {
                binding.ivHomeAvatar.setVisibility(View.VISIBLE);
                binding.tvHomeAvatarInitials.setVisibility(View.GONE);
                Glide.with(this)
                        .load(ProfileUtils.resolveImageUrl(user.getAvatarUrl()))
                        .centerCrop()
                        .into(binding.ivHomeAvatar);
            } else {
                binding.ivHomeAvatar.setVisibility(View.GONE);
                binding.tvHomeAvatarInitials.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupTopBar() {
        binding.btnHomeSearch.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.openSearch();
            }
        });

        binding.btnHomeAvatar.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.openProfile();
            }
        });
    }

    private void setupOnboardingBlocks() {
        binding.btnPersonalizeExams.setOnClickListener(v -> {
            String[] exams = {"IELTS Academic", "TOEIC 750+", "THPT Quốc gia", "Tiếng Nhật JLPT", "Lập trình & CNTT"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Chọn mục tiêu ôn thi")
                    .setItems(exams, (dialog, which) ->
                            Toast.makeText(requireContext(), "Đã chọn mục tiêu: " + exams[which], Toast.LENGTH_SHORT).show()
                    )
                    .setNegativeButton("Đóng", null)
                    .show();
        });

        binding.btnHomeCreateCards.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.showCreateBottomSheet();
            }
        });
    }

    private void setupTriviaBlock() {
        renderTriviaQuestion();
        TextView[] optViews = getTriviaOptionViews();
        for (int i = 0; i < optViews.length; i++) {
            final int optionIdx = i;
            optViews[i].setOnClickListener(v -> onTriviaOptionSelected(optionIdx));
        }
    }

    private TextView[] getTriviaOptionViews() {
        return new TextView[]{
                binding.tvTriviaOpt0,
                binding.tvTriviaOpt1,
                binding.tvTriviaOpt2,
                binding.tvTriviaOpt3
        };
    }

    private void renderTriviaQuestion() {
        triviaLocked = false;
        TriviaQuestion q = TRIVIA_QUESTIONS.get(currentTriviaIndex);
        binding.tvTriviaProgress.setText((currentTriviaIndex + 1) + " / " + TRIVIA_QUESTIONS.size());
        binding.tvTriviaQuestion.setText(q.question);

        TextView[] optViews = getTriviaOptionViews();
        int primaryTextColor = ContextCompat.getColor(requireContext(), R.color.sf_text_primary);
        for (int i = 0; i < optViews.length; i++) {
            optViews[i].setText(q.options[i]);
            optViews[i].setBackgroundResource(R.drawable.bg_trivia_option_normal);
            optViews[i].setTextColor(primaryTextColor);
            optViews[i].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            optViews[i].setCompoundDrawablePadding(0);
        }
    }

    private void onTriviaOptionSelected(int selectedIdx) {
        if (triviaLocked || binding == null) return;
        triviaLocked = true;

        TriviaQuestion q = TRIVIA_QUESTIONS.get(currentTriviaIndex);
        TextView[] optViews = getTriviaOptionViews();
        int densityPadding = Math.round(12 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < optViews.length; i++) {
            if (i == q.correctIndex) {
                optViews[i].setBackgroundResource(R.drawable.bg_trivia_option_correct);
                optViews[i].setTextColor(Color.parseColor("#282E3E"));
                optViews[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_green, 0, 0, 0);
                optViews[i].setCompoundDrawablePadding(densityPadding);
            } else if (i == selectedIdx) {
                optViews[i].setBackgroundResource(R.drawable.bg_trivia_option_wrong);
                optViews[i].setTextColor(Color.parseColor("#282E3E"));
                optViews[i].setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close_orange, 0, 0, 0);
                optViews[i].setCompoundDrawablePadding(densityPadding);
            } else {
                optViews[i].setBackgroundResource(R.drawable.bg_trivia_option_normal);
                optViews[i].setTextColor(Color.parseColor("#C4C9D4"));
                optViews[i].setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }

        handler.postDelayed(() -> {
            if (binding == null) return;
            currentTriviaIndex = (currentTriviaIndex + 1) % TRIVIA_QUESTIONS.size();
            renderTriviaQuestion();
        }, 1100L);
    }

    private void setupStaffPicksBlock() {
        staffPickAdapter = new StaffPickAdapter(item ->
                Toast.makeText(requireContext(), "Xem chi tiết bộ thẻ \"" + item.title + "\" sẽ mở ở Task T3.12.", Toast.LENGTH_SHORT).show()
        );
        binding.rvStaffPicks.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvStaffPicks.setAdapter(staffPickAdapter);

        // Curated starter picks (will be merged with live /flashcard-sets/explore in T3.10)
        staffPickAdapter.submitList(Arrays.asList(
                new StaffPickAdapter.StaffPickItem(1L, "SmartFlash Cơ bản", 11, "SmartFlash"),
                new StaffPickAdapter.StaffPickItem(2L, "1000 Từ vựng IELTS", 45, "ielts_mentor"),
                new StaffPickAdapter.StaffPickItem(3L, "Cấu trúc Dữ liệu & Giải thuật", 32, "cs_club")
        ));
    }

    private void setupGameSwitchBlock() {
        binding.tabGameMatch.setOnClickListener(v -> selectGameTab(0));
        binding.tabGameBlast.setOnClickListener(v -> selectGameTab(1));
        binding.tabGameBlocks.setOnClickListener(v -> selectGameTab(2));

        binding.btnPlayGame.setOnClickListener(v -> {
            String gameName = activeGameTab == 0 ? "Ghép thẻ" : (activeGameTab == 1 ? "Blast" : "Khối hộp");
            Toast.makeText(requireContext(), "Minigame " + gameName + " sẽ mở ở Phase 6 (T6.9).", Toast.LENGTH_SHORT).show();
        });

        selectGameTab(0);
    }

    private void selectGameTab(int index) {
        activeGameTab = index;
        TextView[] tabs = {binding.tabGameMatch, binding.tabGameBlast, binding.tabGameBlocks};
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.sf_text_primary);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.sf_text_secondary);
        for (int i = 0; i < tabs.length; i++) {
            if (i == index) {
                tabs[i].setBackgroundResource(R.drawable.bg_segmented_active);
                tabs[i].setTextColor(primaryColor);
            } else {
                tabs[i].setBackgroundColor(Color.TRANSPARENT);
                tabs[i].setTextColor(secondaryColor);
            }
        }

        if (index == 0) {
            bindGameCard(R.drawable.ic_game_match, "Chào hỏi đa ngôn ngữ",
                    "Ghép cặp thuật ngữ và định nghĩa nhanh nhất có thể",
                    R.drawable.img_game_match, "Chơi Ghép thẻ");
        } else if (index == 1) {
            bindGameCard(R.drawable.ic_game_blast, "Bắn thiên thạch từ vựng",
                    "Bắn hạ đáp án đúng trước khi hết thời gian",
                    R.drawable.img_game_blast, "Chơi Blast");
        } else {
            bindGameCard(R.drawable.ic_game_blocks, "Quốc kỳ thế giới",
                    "Nhận thêm lượt xếp khối khi trả lời đúng câu hỏi",
                    R.drawable.img_game_blocks, "Chơi Khối hộp");
        }
    }

    private void bindGameCard(
            @DrawableRes int iconRes,
            String title,
            String desc,
            @DrawableRes int bannerRes,
            String buttonLabel
    ) {
        binding.ivGameIcon.setImageResource(iconRes);
        binding.tvGameTitle.setText(title);
        binding.tvGameDesc.setText(desc);
        binding.ivGameBanner.setImageResource(bannerRes);
        binding.btnPlayGame.setText(buttonLabel);
    }
}
