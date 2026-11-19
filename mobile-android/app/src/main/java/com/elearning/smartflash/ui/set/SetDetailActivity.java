package com.elearning.smartflash.ui.set;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.FolderApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.FlashcardItemDto;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;
import com.elearning.smartflash.data.remote.dto.FolderDetailDto;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;
import com.elearning.smartflash.databinding.ActivitySetDetailBinding;
import com.elearning.smartflash.ui.folder.CreateFolderActivity;
import com.elearning.smartflash.util.TtsHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Flashcard set detail screen matching Quizlet Android UI:
 * horizontal ViewPager2 preview carousel with horizontal-axis (rotationX) 3D flip,
 * Android TTS pronunciation, BottomSheet save/folder & 3-dot menus, study modes, and terms list.
 */
public class SetDetailActivity extends AppCompatActivity
        implements SetTermAdapter.OnTermActionListener, PreviewDeckAdapter.OnDeckCardListener {

    public static final String EXTRA_SET_ID = "extra_set_id";
    public static final String EXTRA_SHARE_TOKEN = "extra_share_token";

    private ActivitySetDetailBinding binding;
    private SetDetailViewModel viewModel;
    private PreviewDeckAdapter previewDeckAdapter;
    private SetTermAdapter termAdapter;
    private TtsHelper ttsHelper;
    private FolderApiService folderApiService;

    private FlashcardSetDetailDto currentSet;
    private final List<FlashcardItemDto> originalCards = new ArrayList<>();
    private final List<FlashcardItemDto> displayCards = new ArrayList<>();
    private Set<Long> starredIds = new HashSet<>();

    private boolean shuffled = false;
    private boolean filterStarredOnly = false;
    private int currentDeckPosition = 0;

    private final ActivityResultLauncher<Intent> editSetLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentSet != null && currentSet.getId() != null) {
                    viewModel.loadSet(currentSet.getId(), currentSet.getShareToken());
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        syncSystemBarsWithBackground();

        viewModel = new ViewModelProvider(this).get(SetDetailViewModel.class);
        folderApiService = RetrofitClient.getInstance(this).getFolderApi();
        ttsHelper = new TtsHelper();
        ttsHelper.init(this, null);

        setupProgressBadges();
        setupPreviewCarousel();
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        long setId = getIntent().getLongExtra(EXTRA_SET_ID, -1L);
        String shareToken = getIntent().getStringExtra(EXTRA_SHARE_TOKEN);
        viewModel.loadSet(setId, shareToken);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ttsHelper != null) {
            ttsHelper.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        super.onDestroy();
    }

    private void syncSystemBarsWithBackground() {
        Window window = getWindow();
        int bgColor = ContextCompat.getColor(this, R.color.quizlet_bg);
        window.setStatusBarColor(bgColor);
        window.setNavigationBarColor(bgColor);
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!isNight);
        controller.setAppearanceLightNavigationBars(!isNight);
    }

    private void setupProgressBadges() {
        binding.progressNotStudied.setColors(0xFFD6DAE8, 0xFF2E3856);
        binding.progressLearning.setColors(0xFFFDE4CE, 0xFFF97316);
        binding.progressMastered.setColors(0xFFD1FAE5, 0xFF23B26D);
    }

    private void setupPreviewCarousel() {
        previewDeckAdapter = new PreviewDeckAdapter(this);
        binding.vpPreviewDeck.setAdapter(previewDeckAdapter);
        binding.vpPreviewDeck.setOffscreenPageLimit(2);
        binding.vpPreviewDeck.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentDeckPosition = position;
                renderDotIndicators(displayCards.size(), position);
            }
        });
    }

    private void setupRecyclerView() {
        termAdapter = new SetTermAdapter(this);
        binding.rvSetTerms.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSetTerms.setAdapter(termAdapter);
    }

    private void setupListeners() {
        binding.btnDetailBack.setOnClickListener(v -> finish());
        binding.btnDetailSave.setOnClickListener(v -> showSaveBottomSheet());
        binding.btnDetailMore.setOnClickListener(v -> showMoreBottomSheet());
        binding.btnDetailRetry.setOnClickListener(v -> {
            long setId = getIntent().getLongExtra(EXTRA_SET_ID, -1L);
            String shareToken = getIntent().getStringExtra(EXTRA_SHARE_TOKEN);
            viewModel.loadSet(setId, shareToken);
        });

        binding.llTermsFilter.setOnClickListener(v -> showTermsFilterDialog());
        binding.btnStudyThisSet.setOnClickListener(v -> openStudyMode("FLASHCARDS"));

        binding.btnModeFlashcards.setOnClickListener(v -> openStudyMode("FLASHCARDS"));
        binding.btnModeLearn.setOnClickListener(v -> openStudyMode("LEARN"));
        binding.btnModeTest.setOnClickListener(v -> openStudyMode("TEST"));
        binding.btnModeMatch.setOnClickListener(v -> openStudyMode("MATCH"));
        binding.btnModeBlast.setOnClickListener(v -> openStudyMode("BLAST"));
        binding.btnModeBlocks.setOnClickListener(v -> openStudyMode("BLOCKS"));
        binding.btnModeLive.setOnClickListener(v -> openStudyMode("LIVE"));
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.pbDetailLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.llDetailError.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (!TextUtils.isEmpty(msg)) {
                binding.svDetailContent.setVisibility(View.GONE);
                binding.flBottomCta.setVisibility(View.GONE);
                binding.llDetailError.setVisibility(View.VISIBLE);
                binding.tvDetailError.setText(msg);
            }
        });

        viewModel.getSetDetail().observe(this, set -> {
            if (set == null) return;
            currentSet = set;
            binding.llDetailError.setVisibility(View.GONE);
            binding.svDetailContent.setVisibility(View.VISIBLE);
            binding.flBottomCta.setVisibility(View.VISIBLE);
            renderSetHeader(set);
            bindCards(set.getCards());
        });

        viewModel.getSaved().observe(this, isSaved -> {
            boolean saved = Boolean.TRUE.equals(isSaved);
            binding.btnDetailSave.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
            binding.btnDetailSave.setColorFilter(ContextCompat.getColor(
                    this,
                    saved ? R.color.quizlet_blue : R.color.sf_text_primary
            ));
        });

        viewModel.getStarredCardIds().observe(this, ids -> {
            starredIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
            refreshTermList();
        });

        viewModel.getActionMessage().observe(this, msg -> {
            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearActionMessage();
            }
        });

        viewModel.getClonedSetId().observe(this, newSetId -> {
            if (newSetId != null && newSetId > 0) {
                Intent intent = new Intent(this, SetDetailActivity.class);
                intent.putExtra(EXTRA_SET_ID, newSetId);
                startActivity(intent);
            }
        });

        viewModel.getDeletedSuccess().observe(this, deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                Toast.makeText(this, "Đã xóa học phần", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void renderSetHeader(FlashcardSetDetailDto set) {
        binding.tvDetailTitle.setText(!TextUtils.isEmpty(set.getTitle()) ? set.getTitle() : "Học phần");
        binding.tvDetailDescription.setVisibility(View.GONE);

        SetSummaryDto.OwnerSummaryDto owner = set.getOwner();
        if (owner != null) {
            String authorName = !TextUtils.isEmpty(owner.getFullName())
                    ? owner.getFullName()
                    : (!TextUtils.isEmpty(owner.getUsername()) ? owner.getUsername() : "Người dùng");
            binding.tvDetailAuthorName.setText(authorName);
            binding.ivDetailVerified.setVisibility(owner.isVerifiedTeacher() ? View.VISIBLE : View.GONE);
            if (!TextUtils.isEmpty(owner.getAvatarUrl())) {
                Glide.with(this)
                        .load(owner.getAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.bg_circle_avatar)
                        .into(binding.ivDetailAuthorAvatar);
            } else {
                binding.ivDetailAuthorAvatar.setImageResource(R.drawable.bg_circle_avatar);
            }
        } else {
            binding.tvDetailAuthorName.setText("SmartFlash");
            binding.ivDetailVerified.setVisibility(View.GONE);
        }

        int count = set.getCardCount() != null
                ? set.getCardCount()
                : (set.getCards() != null ? set.getCards().size() : 0);
        binding.tvDetailMeta.setText(count + " thuật ngữ");
        binding.tvTermsSectionTitle.setText("Thuật ngữ");
        binding.progressNotStudied.setProgress(count, count);
        binding.progressLearning.setProgress(0, count);
        binding.progressMastered.setProgress(0, count);
    }

    private void bindCards(List<FlashcardItemDto> cards) {
        originalCards.clear();
        if (cards != null) {
            originalCards.addAll(cards);
        }
        rebuildDisplayCards();
        refreshTermList();
    }

    private void rebuildDisplayCards() {
        displayCards.clear();
        displayCards.addAll(originalCards);
        if (shuffled && displayCards.size() > 1) {
            Collections.shuffle(displayCards);
        }
        previewDeckAdapter.submitCards(displayCards);
        currentDeckPosition = 0;
        if (!displayCards.isEmpty()) {
            binding.vpPreviewDeck.setCurrentItem(0, false);
        }
        renderDotIndicators(displayCards.size(), 0);
    }

    private void renderDotIndicators(int totalCount, int activePos) {
        binding.llDeckDots.removeAllViews();
        if (totalCount <= 1) return;

        final int maxDots = 5;
        int visibleDots = Math.min(totalCount, maxDots);
        int windowStart = totalCount <= maxDots
                ? 0
                : Math.max(0, Math.min(activePos - 2, totalCount - maxDots));
        int activeDotIndex = activePos - windowStart;

        float density = getResources().getDisplayMetrics().density;
        int activeColor = ContextCompat.getColor(this, R.color.quizlet_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.sf_underline_idle);

        for (int i = 0; i < visibleDots; i++) {
            View dot = new View(this);
            boolean isActive = (i == activeDotIndex);
            boolean isEdgeOverflow = (totalCount > maxDots) && (
                    (i == 0 && windowStart > 0) ||
                    (i == visibleDots - 1 && windowStart + visibleDots < totalCount)
            );

            float sizeDp = isActive ? 7.5f : (isEdgeOverflow ? 4.5f : 6f);
            int sizePx = Math.round(sizeDp * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(Math.round(4f * density), 0, Math.round(4f * density), 0);
            dot.setLayoutParams(lp);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(isActive ? activeColor : inactiveColor);
            dot.setBackground(circle);
            binding.llDeckDots.addView(dot);
        }
    }

    private void refreshTermList() {
        List<FlashcardItemDto> source = shuffled ? displayCards : originalCards;
        if (!filterStarredOnly) {
            termAdapter.submitData(source, starredIds);
            binding.tvTermsFilterLabel.setText(shuffled ? "Đã trộn" : "Gốc");
            return;
        }
        List<FlashcardItemDto> filtered = new ArrayList<>();
        for (FlashcardItemDto card : source) {
            if (card.getId() != null && starredIds.contains(card.getId())) {
                filtered.add(card);
            }
        }
        termAdapter.submitData(filtered, starredIds);
        binding.tvTermsFilterLabel.setText("Gắn sao (" + filtered.size() + ")");
    }

    private void showTermsFilterDialog() {
        int starredCount = 0;
        for (FlashcardItemDto card : originalCards) {
            if (card.getId() != null && starredIds.contains(card.getId())) {
                starredCount++;
            }
        }
        String[] options = {
                "Thứ tự gốc",
                "Đã gắn sao (" + starredCount + ")",
                shuffled ? "Tắt trộn thứ tự" : "Trộn thứ tự thẻ"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lọc & sắp xếp thuật ngữ")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        filterStarredOnly = false;
                        shuffled = false;
                        rebuildDisplayCards();
                        refreshTermList();
                    } else if (which == 1) {
                        filterStarredOnly = true;
                        refreshTermList();
                    } else {
                        shuffled = !shuffled;
                        rebuildDisplayCards();
                        refreshTermList();
                    }
                })
                .show();
    }

    /**
     * Image 2: BottomSheet when tapping Bookmark icon (+ New folder, Save to Library, Folders list).
     */
    private void showSaveBottomSheet() {
        if (currentSet == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_save_set, null);
        sheet.setContentView(view);

        LinearLayout rowNewFolder = view.findViewById(R.id.row_save_new_folder);
        LinearLayout rowSaveLibrary = view.findViewById(R.id.row_save_to_library);
        ImageView ivSaveIcon = view.findViewById(R.id.iv_save_library_icon);
        TextView tvSaveLabel = view.findViewById(R.id.tv_save_library_label);
        LinearLayout foldersContainer = view.findViewById(R.id.ll_save_folders_container);

        boolean currentlySaved = Boolean.TRUE.equals(viewModel.getSaved().getValue());
        ivSaveIcon.setImageResource(currentlySaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_circle_outline);
        ivSaveIcon.setColorFilter(ContextCompat.getColor(
                this,
                currentlySaved ? R.color.quizlet_blue : R.color.sf_text_secondary
        ));
        tvSaveLabel.setText(currentlySaved ? "Đã lưu trong thư viện" : "Lưu vào thư viện");

        rowNewFolder.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, CreateFolderActivity.class));
        });

        rowSaveLibrary.setOnClickListener(v -> {
            sheet.dismiss();
            viewModel.toggleSave();
        });

        if (TokenManager.getInstance(this).isLoggedIn() && currentSet.getId() != null) {
            loadFoldersIntoSheet(foldersContainer, sheet);
        }

        applyTransparentSheetBackground(sheet);
        sheet.show();
    }

    private void applyTransparentSheetBackground(BottomSheetDialog sheet) {
        sheet.setOnShowListener(dialog -> {
            View bottomSheet = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        });
    }

    private void loadFoldersIntoSheet(LinearLayout container, BottomSheetDialog sheet) {
        folderApiService.getMyFolders(0, 15).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PageResponseDto<FolderSummaryDto>>> call,
                                   Response<ApiResponse<PageResponseDto<FolderSummaryDto>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    return;
                }
                List<FolderSummaryDto> folders = response.body().getData().getContent();
                if (folders == null || folders.isEmpty()) return;

                float density = getResources().getDisplayMetrics().density;
                for (FolderSummaryDto folder : folders) {
                    if (folder.getId() == null) continue;
                    LinearLayout row = new LinearLayout(SetDetailActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(56f * density)
                    );
                    lp.topMargin = Math.round(10f * density);
                    row.setLayoutParams(lp);
                    row.setBackgroundResource(R.drawable.bg_save_sheet_item);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding(Math.round(16f * density), 0, Math.round(16f * density), 0);

                    ImageView icon = new ImageView(SetDetailActivity.this);
                    icon.setLayoutParams(new LinearLayout.LayoutParams(
                            Math.round(22f * density), Math.round(22f * density)));
                    icon.setImageResource(R.drawable.ic_circle_outline);
                    icon.setColorFilter(ContextCompat.getColor(SetDetailActivity.this, R.color.sf_text_secondary));

                    TextView title = new TextView(SetDetailActivity.this);
                    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    titleLp.setMarginStart(Math.round(14f * density));
                    title.setLayoutParams(titleLp);
                    title.setText(folder.getName());
                    title.setTextColor(ContextCompat.getColor(SetDetailActivity.this, R.color.sf_text_primary));
                    title.setTextSize(15f);
                    title.setTypeface(null, android.graphics.Typeface.BOLD);

                    ImageView chevron = new ImageView(SetDetailActivity.this);
                    chevron.setLayoutParams(new LinearLayout.LayoutParams(
                            Math.round(18f * density), Math.round(18f * density)));
                    chevron.setImageResource(R.drawable.ic_chevron_right);
                    chevron.setColorFilter(ContextCompat.getColor(SetDetailActivity.this, R.color.sf_text_secondary));

                    row.addView(icon);
                    row.addView(title);
                    row.addView(chevron);
                    row.setOnClickListener(v -> addSetToFolder(folder.getId(), folder.getName(), sheet));
                    container.addView(row);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<PageResponseDto<FolderSummaryDto>>> call, Throwable t) {
                // Non-fatal
            }
        });
    }

    private void addSetToFolder(long folderId, String folderName, BottomSheetDialog sheet) {
        if (currentSet == null || currentSet.getId() == null) return;
        folderApiService.addSetToFolder(folderId, currentSet.getId()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<FolderDetailDto>> call, Response<ApiResponse<FolderDetailDto>> response) {
                sheet.dismiss();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SetDetailActivity.this, "Đã thêm vào thư mục \"" + folderName + "\"", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SetDetailActivity.this, "Không thể thêm vào thư mục", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FolderDetailDto>> call, Throwable t) {
                sheet.dismiss();
                Toast.makeText(SetDetailActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Image 3: BottomSheet when tapping 3-dot More button.
     */
    private void showMoreBottomSheet() {
        if (currentSet == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_set_more, null);
        sheet.setContentView(view);

        View rowSaveFolder = view.findViewById(R.id.row_more_save_folder);
        View rowEdit = view.findViewById(R.id.row_more_edit);
        View rowClone = view.findViewById(R.id.row_more_clone);
        View rowShare = view.findViewById(R.id.row_more_share);
        View rowRate = view.findViewById(R.id.row_more_rate);
        View rowDelete = view.findViewById(R.id.row_more_delete);

        boolean isOwner = currentSet.isOwnerView() && currentSet.getId() != null;
        rowEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        rowDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        rowSaveFolder.setOnClickListener(v -> {
            sheet.dismiss();
            showSaveBottomSheet();
        });

        rowEdit.setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(this, CreateEditSetActivity.class);
            intent.putExtra(CreateEditSetActivity.EXTRA_SET_ID, currentSet.getId());
            editSetLauncher.launch(intent);
        });

        rowClone.setOnClickListener(v -> {
            sheet.dismiss();
            viewModel.cloneSet();
        });

        rowShare.setOnClickListener(v -> {
            sheet.dismiss();
            shareSetLink();
        });

        rowRate.setOnClickListener(v -> {
            sheet.dismiss();
            showRateDialog();
        });

        rowDelete.setOnClickListener(v -> {
            sheet.dismiss();
            confirmDeleteSet();
        });

        applyTransparentSheetBackground(sheet);
        sheet.show();
    }

    private String buildShareUrl() {
        if (currentSet == null) return "";
        if (!TextUtils.isEmpty(currentSet.getShareToken())) {
            return "https://smartflash.app/sets/share/" + currentSet.getShareToken();
        }
        return "https://smartflash.app/sets/" + currentSet.getId();
    }

    private void shareSetLink() {
        if (currentSet == null) return;
        String url = buildShareUrl();
        String title = !TextUtils.isEmpty(currentSet.getTitle()) ? currentSet.getTitle() : "Học phần SmartFlash";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Học cùng mình bộ thẻ \"" + title + "\" trên SmartFlash:\n" + url);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ học phần"));
    }

    private void showRateDialog() {
        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        Integer existing = viewModel.getUserRating().getValue();
        ratingBar.setRating(existing != null && existing > 0 ? existing : 5f);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đánh giá học phần")
                .setView(ratingBar)
                .setPositiveButton("Gửi đánh giá", (d, w) -> {
                    int stars = Math.max(1, Math.round(ratingBar.getRating()));
                    viewModel.rateSet(stars, null);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteSet() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa học phần?")
                .setMessage("Bạn có chắc chắn muốn xóa học phần này? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteSet())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openStudyMode(String mode) {
        if (currentSet == null || originalCards.isEmpty()) {
            Toast.makeText(this, "Học phần chưa có thẻ để học", Toast.LENGTH_SHORT).show();
            return;
        }
        String label;
        switch (mode) {
            case "FLASHCARDS":
                onOpenFullscreen(currentDeckPosition, false);
                return;
            case "LEARN":
                label = "Chế độ Học thích ứng sẽ mở ở Phase 6 (T6.8).";
                break;
            case "TEST":
                label = "Chế độ Kiểm tra sẽ mở ở Phase 6 (T6.8).";
                break;
            case "MATCH":
                label = "Minigame Ghép thẻ sẽ mở ở Phase 6 (T6.9).";
                break;
            case "BLAST":
                label = "Minigame Blast sẽ mở ở Phase 6 (T6.9).";
                break;
            case "BLOCKS":
                label = "Minigame Khối hộp sẽ mở ở Phase 6.";
                break;
            case "LIVE":
                label = "Đấu trường Live sẽ mở ở Phase 6 (T6.10).";
                break;
            default:
                label = "Tính năng đang phát triển.";
                break;
        }
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSpeakTerm(FlashcardItemDto card) {
        if (card == null || TextUtils.isEmpty(card.getTerm())) return;
        String lang = currentSet != null ? currentSet.getTermLang() : "en";
        if (!ttsHelper.speak(card.getTerm(), lang)) {
            Toast.makeText(this, "Thiết bị chưa hỗ trợ phát âm TTS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSpeakDefinition(FlashcardItemDto card) {
        if (card == null || TextUtils.isEmpty(card.getDefinition())) return;
        String lang = currentSet != null ? currentSet.getDefLang() : "vi";
        if (!ttsHelper.speak(card.getDefinition(), lang)) {
            Toast.makeText(this, "Thiết bị chưa hỗ trợ phát âm TTS", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onToggleStar(FlashcardItemDto card) {
        if (card == null || card.getId() == null) return;
        viewModel.toggleStarCard(card.getId());
    }

    @Override
    public void onOpenFullscreen(int position, boolean showingDefinition) {
        if (displayCards.isEmpty()) return;
        String termLang = currentSet != null ? currentSet.getTermLang() : "en";
        String defLang = currentSet != null ? currentSet.getDefLang() : "vi";
        FullscreenFlashcardDialog dialog = new FullscreenFlashcardDialog(
                this,
                displayCards,
                position,
                showingDefinition,
                termLang,
                defLang,
                ttsHelper,
                (notStudied, learning, known) -> {
                    int total = displayCards.size();
                    binding.progressNotStudied.setProgress(notStudied, total);
                    binding.progressLearning.setProgress(learning, total);
                    binding.progressMastered.setProgress(known, total);
                }
        );
        dialog.show();
    }
}
