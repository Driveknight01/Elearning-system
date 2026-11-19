package com.elearning.smartflash.ui.set;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.MediaUploadResponseDto;
import com.elearning.smartflash.databinding.ActivityCreateEditSetBinding;
import com.elearning.smartflash.util.ProfileUtils;
import com.elearning.smartflash.util.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Quizlet-style Create / Edit Flashcard Set screen matching the mobile reference design.
 */
public class CreateEditSetActivity extends AppCompatActivity implements FlashcardEditAdapter.Callbacks {

    public static final String EXTRA_SET_ID = "extra_set_id";
    public static final String RESULT_SAVED_SET_ID = "result_saved_set_id";

    private ActivityCreateEditSetBinding binding;
    private CreateEditSetViewModel viewModel;
    private FlashcardEditAdapter adapter;
    private LinearLayoutManager layoutManager;
    private ItemTouchHelper itemTouchHelper;
    private int pendingInsertAfterCardIndex = -1;
    private int pendingImageCardIndex = -1;

    private final ActivityResultLauncher<String> cardImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && pendingImageCardIndex >= 0
                        && pendingImageCardIndex < viewModel.getCards().size()) {
                    uploadCardImageFromUri(pendingImageCardIndex, uri);
                }
                pendingImageCardIndex = -1;
            });

    private final ActivityResultLauncher<Intent> optionsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Intent data = result.getData();
                if (data.getBooleanExtra(SetOptionsActivity.EXTRA_DELETE_SET, false)) {
                    viewModel.deleteSetIfEditing();
                    return;
                }
                viewModel.setTermLang(data.getStringExtra(SetOptionsActivity.EXTRA_TERM_LANG));
                viewModel.setDefLang(data.getStringExtra(SetOptionsActivity.EXTRA_DEF_LANG));
                viewModel.setAutoSuggest(data.getBooleanExtra(SetOptionsActivity.EXTRA_AUTO_SUGGEST, true));
                viewModel.setVisibility(data.getStringExtra(SetOptionsActivity.EXTRA_VISIBILITY));
                viewModel.setEditableBy(data.getStringExtra(SetOptionsActivity.EXTRA_EDITABLE_BY));
                adapter.notifyDataSetChanged();
            });

    private final ActivityResultLauncher<Intent> languageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String target = result.getData().getStringExtra(SelectLanguageActivity.EXTRA_TARGET);
                String selectedCode = result.getData().getStringExtra(SelectLanguageActivity.EXTRA_SELECTED_CODE);
                if ("term".equals(target)) {
                    viewModel.setTermLang(selectedCode);
                } else if ("def".equals(target)) {
                    viewModel.setDefLang(selectedCode);
                }
                adapter.notifyDataSetChanged();
            });

    private final ActivityResultLauncher<Intent> scanDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                ArrayList<String> terms = result.getData().getStringArrayListExtra(ScanDocumentActivity.EXTRA_SCANNED_TERMS);
                ArrayList<String> defs = result.getData().getStringArrayListExtra(ScanDocumentActivity.EXTRA_SCANNED_DEFS);
                if (terms == null || defs == null || terms.isEmpty()) {
                    return;
                }
                mergeScannedCards(terms, defs);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();
        binding = ActivityCreateEditSetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreateEditSetViewModel.class);

        setupRecyclerView();
        setupGapHoldInsertOverlay();
        setupToolbarAndFab();
        observeViewModel();

        long setId = getIntent().getLongExtra(EXTRA_SET_ID, -1L);
        if (setId > 0) {
            binding.tvToolbarTitle.setText("Edit set");
            viewModel.loadSetForEdit(setId, () -> runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                binding.tvToolbarTitle.setText("Edit set");
            }));
        }
    }

    private void applyStatusBar() {
        Window window = getWindow();
        if (window == null) return;
        boolean nightMode = ThemeManager.isNightMode(this);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_surface));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.quizlet_bg));
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(!nightMode);
    }

    private void setupRecyclerView() {
        adapter = new FlashcardEditAdapter(viewModel, this);
        layoutManager = new LinearLayoutManager(this);
        binding.rvCards.setLayoutManager(layoutManager);
        binding.rvCards.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeAndDragCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == FlashcardEditAdapter.TYPE_HEADER) {
                    return makeMovementFlags(0, 0);
                }
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                if (target.getItemViewType() == FlashcardEditAdapter.TYPE_HEADER) {
                    return false;
                }
                int fromAdapterPos = viewHolder.getBindingAdapterPosition();
                int toAdapterPos = target.getBindingAdapterPosition();
                if (fromAdapterPos <= 0 || toAdapterPos <= 0) {
                    return false;
                }
                int fromCard = fromAdapterPos - 1;
                int toCard = toAdapterPos - 1;
                List<EditableCardItem> cards = viewModel.getCards();
                if (fromCard < cards.size() && toCard < cards.size()) {
                    Collections.swap(cards, fromCard, toCard);
                    adapter.notifyItemMoved(fromAdapterPos, toAdapterPos);
                    return true;
                }
                return false;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.92f);
                    viewHolder.itemView.setScaleX(1.02f);
                    viewHolder.itemView.setScaleY(1.02f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                adapter.notifyItemRangeChanged(1, viewModel.getCards().size());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int adapterPos = viewHolder.getBindingAdapterPosition();
                int cardIndex = adapterPos - 1;
                if (cardIndex < 0 || cardIndex >= viewModel.getCards().size()) {
                    return;
                }
                if (viewModel.getCards().size() <= 1) {
                    adapter.notifyItemChanged(adapterPos);
                    Toast.makeText(CreateEditSetActivity.this,
                            "A set must have at least 1 card", Toast.LENGTH_SHORT).show();
                    return;
                }
                EditableCardItem removed = viewModel.getCards().remove(cardIndex);
                adapter.notifyItemRemoved(adapterPos);
                if (!viewModel.getCards().isEmpty()) {
                    adapter.notifyItemChanged(viewModel.getCards().size());
                }
                Snackbar.make(binding.getRoot(), "Card removed", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            int restoreIndex = Math.min(cardIndex, viewModel.getCards().size());
                            viewModel.getCards().add(restoreIndex, removed);
                            adapter.notifyItemInserted(restoreIndex + 1);
                            adapter.notifyItemRangeChanged(1, viewModel.getCards().size());
                        })
                        .show();
            }
        };
        itemTouchHelper = new ItemTouchHelper(swipeAndDragCallback);
        itemTouchHelper.attachToRecyclerView(binding.rvCards);
    }

    private void setupGapHoldInsertOverlay() {
        float density = getResources().getDisplayMetrics().density;
        float gapTolerancePx = 16f * density;

        GestureDetector gapGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public void onLongPress(@NonNull MotionEvent e) {
                        showInsertOverlayIfInGap(e.getY(), gapTolerancePx);
                    }
                });

        binding.rvCards.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    hideInsertOverlay();
                }
                gapGestureDetector.onTouchEvent(e);
                return false;
            }
        });

        binding.rvCards.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (Math.abs(dy) > 4) {
                    hideInsertOverlay();
                }
            }
        });

        binding.btnInsertCardOverlay.setOnClickListener(v -> {
            int targetCardIndex = pendingInsertAfterCardIndex;
            hideInsertOverlay();
            if (targetCardIndex >= 0) {
                insertCardAfter(targetCardIndex);
            }
        });
    }

    private void showInsertOverlayIfInGap(float touchY, float tolerancePx) {
        int childCount = binding.rvCards.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View topView = binding.rvCards.getChildAt(i);
            View bottomView = binding.rvCards.getChildAt(i + 1);
            RecyclerView.ViewHolder topHolder = binding.rvCards.getChildViewHolder(topView);
            RecyclerView.ViewHolder bottomHolder = binding.rvCards.getChildViewHolder(bottomView);

            if (topHolder.getItemViewType() != FlashcardEditAdapter.TYPE_CARD
                    || bottomHolder.getItemViewType() != FlashcardEditAdapter.TYPE_CARD) {
                continue;
            }

            float gapTop = topView.getBottom() - tolerancePx;
            float gapBottom = bottomView.getTop() + tolerancePx;
            if (touchY >= gapTop && touchY <= gapBottom) {
                int topCardIndex = topHolder.getBindingAdapterPosition() - 1;
                if (topCardIndex < 0) {
                    return;
                }
                pendingInsertAfterCardIndex = topCardIndex;
                float centerY = (topView.getBottom() + bottomView.getTop()) / 2f;

                binding.rvCards.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                View overlay = binding.btnInsertCardOverlay;
                overlay.setVisibility(View.VISIBLE);
                overlay.post(() -> {
                    int rvW = binding.rvCards.getWidth();
                    overlay.setTranslationX((rvW - overlay.getWidth()) / 2f);
                    overlay.setTranslationY(centerY - overlay.getHeight() / 2f);
                    overlay.setScaleX(0.6f);
                    overlay.setScaleY(0.6f);
                    overlay.setAlpha(0f);
                    overlay.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(140)
                            .start();
                });
                return;
            }
        }
    }

    private void hideInsertOverlay() {
        pendingInsertAfterCardIndex = -1;
        if (binding.btnInsertCardOverlay.getVisibility() == View.VISIBLE) {
            binding.btnInsertCardOverlay.setVisibility(View.GONE);
        }
    }

    private void setupToolbarAndFab() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SetOptionsActivity.class);
            intent.putExtra(SetOptionsActivity.EXTRA_TERM_LANG, viewModel.getTermLang());
            intent.putExtra(SetOptionsActivity.EXTRA_DEF_LANG, viewModel.getDefLang());
            intent.putExtra(SetOptionsActivity.EXTRA_AUTO_SUGGEST, viewModel.isAutoSuggest());
            intent.putExtra(SetOptionsActivity.EXTRA_VISIBILITY, viewModel.getVisibility());
            intent.putExtra(SetOptionsActivity.EXTRA_EDITABLE_BY, viewModel.getEditableBy());
            intent.putExtra(SetOptionsActivity.EXTRA_IS_EDIT_MODE, viewModel.isEditMode());
            optionsLauncher.launch(intent);
        });

        binding.btnSave.setOnClickListener(v -> viewModel.saveSet());

        binding.fabAddCard.setOnClickListener(v -> {
            int newCardIndex = viewModel.addEmptyCard();
            int adapterPos = newCardIndex + 1;
            adapter.requestFocusForCard(newCardIndex);
            adapter.notifyItemInserted(adapterPos);
            if (adapterPos > 1) {
                adapter.notifyItemChanged(adapterPos - 1);
            }
            binding.rvCards.smoothScrollToPosition(adapterPos);
            binding.tvToolbarTitle.setText((newCardIndex + 1) + "/" + viewModel.getCards().size());
        });
    }

    private void observeViewModel() {
        viewModel.getSaving().observe(this, isSaving -> {
            boolean busy = Boolean.TRUE.equals(isSaving);
            binding.btnSave.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
            binding.btnSave.setEnabled(!busy);
            binding.progressSaving.setVisibility(busy ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        viewModel.getSavedSet().observe(this, saved -> {
            if (saved != null) {
                Toast.makeText(this,
                        viewModel.isEditMode() ? "Set updated" : "Set created",
                        Toast.LENGTH_SHORT).show();
                Intent result = new Intent();
                if (saved.getId() != null) {
                    result.putExtra(RESULT_SAVED_SET_ID, saved.getId());
                }
                setResult(RESULT_OK, result);
                finish();
            }
        });

        viewModel.getDeleted().observe(this, isDeleted -> {
            if (Boolean.TRUE.equals(isDeleted)) {
                Toast.makeText(this,
                        viewModel.isEditMode() ? "Set deleted" : "Set discarded",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onTitleChanged(String title) {
        viewModel.setTitle(title);
    }

    @Override
    public void onDescriptionChanged(String description) {
        viewModel.setDescription(description);
    }

    @Override
    public void onDescriptionShown() {
        viewModel.setDescriptionVisible(true);
    }

    @Override
    public void onScanDocumentClicked() {
        Intent intent = new Intent(this, ScanDocumentActivity.class);
        intent.putExtra(ScanDocumentActivity.EXTRA_TERM_LANG, viewModel.getTermLang());
        intent.putExtra(ScanDocumentActivity.EXTRA_DEF_LANG, viewModel.getDefLang());
        intent.putExtra(ScanDocumentActivity.EXTRA_LIVE_RECEIVER, new ResultReceiver(
                new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == RESULT_OK && resultData != null) {
                    ArrayList<String> terms = resultData.getStringArrayList(
                            ScanDocumentActivity.EXTRA_SCANNED_TERMS);
                    ArrayList<String> defs = resultData.getStringArrayList(
                            ScanDocumentActivity.EXTRA_SCANNED_DEFS);
                    if (terms != null && defs != null && !terms.isEmpty()) {
                        mergeScannedCards(terms, defs);
                    }
                }
            }
        });
        scanDocumentLauncher.launch(intent);
    }

    private void mergeScannedCards(List<String> terms, List<String> defs) {
        List<EditableCardItem> cards = viewModel.getCards();
        int count = Math.min(terms.size(), defs.size());
        for (int i = 0; i < count; i++) {
            String t = terms.get(i);
            String d = defs.get(i);
            boolean filledExisting = false;
            for (EditableCardItem existing : cards) {
                if (existing.getTerm().trim().isEmpty() && existing.getDefinition().trim().isEmpty()) {
                    existing.setTerm(t);
                    existing.setDefinition(d);
                    filledExisting = true;
                    break;
                }
            }
            if (!filledExisting) {
                cards.add(new EditableCardItem(null, t, d, null));
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Đã thêm " + count + " thẻ từ tài liệu quét", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHeaderFocused() {
        binding.tvToolbarTitle.setText(viewModel.isEditMode() ? "Edit set" : "Create set");
    }

    @Override
    public void onCardFocused(int cardNumberOneBased, int totalCards) {
        binding.tvToolbarTitle.setText(cardNumberOneBased + "/" + totalCards);
    }

    @Override
    public void onSelectTermLanguageClicked(int cardIndex) {
        Intent intent = new Intent(this, SelectLanguageActivity.class);
        intent.putExtra(SelectLanguageActivity.EXTRA_HEADER_TITLE, "TERM LANGUAGE");
        intent.putExtra(SelectLanguageActivity.EXTRA_TARGET, "term");
        intent.putExtra(SelectLanguageActivity.EXTRA_CURRENT_CODE, viewModel.getTermLang());
        languageLauncher.launch(intent);
    }

    @Override
    public void onSelectDefLanguageClicked(int cardIndex) {
        Intent intent = new Intent(this, SelectLanguageActivity.class);
        intent.putExtra(SelectLanguageActivity.EXTRA_HEADER_TITLE, "DEFINITION LANGUAGE");
        intent.putExtra(SelectLanguageActivity.EXTRA_TARGET, "def");
        intent.putExtra(SelectLanguageActivity.EXTRA_CURRENT_CODE, viewModel.getDefLang());
        languageLauncher.launch(intent);
    }

    @Override
    public void onCardCameraClicked(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= viewModel.getCards().size()) {
            return;
        }
        EditableCardItem card = viewModel.getCards().get(cardIndex);
        boolean hasImage = card.getImageUrl() != null && !card.getImageUrl().trim().isEmpty();
        String[] options = hasImage
                ? new String[]{"Chọn ảnh từ máy", "Nhập URL ảnh", "Xóa ảnh"}
                : new String[]{"Chọn ảnh từ máy", "Nhập URL ảnh"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Ảnh minh họa thẻ")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pendingImageCardIndex = cardIndex;
                        cardImagePickerLauncher.launch("image/*");
                    } else if (which == 1) {
                        showCardImageUrlDialog(cardIndex, card);
                    } else if (which == 2) {
                        card.setImageUrl(null);
                        adapter.notifyItemChanged(cardIndex + 1);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCardImageUrlDialog(int cardIndex, EditableCardItem card) {
        EditText input = new EditText(this);
        input.setHint("https://example.com/image.jpg");
        if (card.getImageUrl() != null) {
            input.setText(card.getImageUrl());
        }
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Nhập URL ảnh")
                .setView(input)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    card.setImageUrl(url.isEmpty() ? null : url);
                    adapter.notifyItemChanged(cardIndex + 1);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void uploadCardImageFromUri(int cardIndex, Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            byte[] bytes = buffer.toByteArray();
            RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
            MultipartBody.Part part = MultipartBody.Part.createFormData(
                    "file", "card_" + System.currentTimeMillis() + ".jpg", body);

            Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
            RetrofitClient.getInstance(this).getMediaApi().uploadCardImage(part).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<MediaUploadResponseDto>> call,
                                       @NonNull Response<ApiResponse<MediaUploadResponseDto>> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess() && response.body().getData() != null) {
                        String resolved = ProfileUtils.resolveImageUrl(response.body().getData().getUrl());
                        if (cardIndex >= 0 && cardIndex < viewModel.getCards().size()) {
                            viewModel.getCards().get(cardIndex).setImageUrl(resolved);
                            adapter.notifyItemChanged(cardIndex + 1);
                            Toast.makeText(CreateEditSetActivity.this, "Đã gắn ảnh cho thẻ", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CreateEditSetActivity.this, "Tải ảnh thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<MediaUploadResponseDto>> call, @NonNull Throwable t) {
                    Toast.makeText(CreateEditSetActivity.this,
                            "Lỗi tải ảnh: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Không đọc được tệp ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        hideInsertOverlay();
        if (itemTouchHelper != null) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    private void insertCardAfter(int cardIndex) {
        int insertedCardIndex = viewModel.insertEmptyCardAt(cardIndex + 1);
        int adapterPos = insertedCardIndex + 1;
        adapter.requestFocusForCard(insertedCardIndex);
        adapter.notifyItemInserted(adapterPos);
        binding.rvCards.smoothScrollToPosition(adapterPos);
        binding.tvToolbarTitle.setText((insertedCardIndex + 1) + "/" + viewModel.getCards().size());
    }
}
