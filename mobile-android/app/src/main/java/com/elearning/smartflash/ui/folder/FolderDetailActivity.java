package com.elearning.smartflash.ui.folder;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.FlashcardSetApiService;
import com.elearning.smartflash.data.remote.api.FolderApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.FolderDetailDto;
import com.elearning.smartflash.data.remote.dto.FolderRequestDto;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.databinding.ActivityFolderDetailBinding;
import com.elearning.smartflash.ui.library.LibrarySetAdapter;
import com.elearning.smartflash.ui.set.CreateEditSetActivity;
import com.elearning.smartflash.ui.set.SetDetailActivity;
import com.elearning.smartflash.util.ThemeManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Quizlet-style Folder Detail screen supporting:
 * - Viewing folder sets and empty state
 * - Adding study materials (multi-select sets + search + Create new bottom sheet)
 * - Tag chips (+ New tag modal)
 * - 3-dot folder options (Add materials, Add tag, Edit, Share, Delete)
 */
public class FolderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOLDER_ID = "extra_folder_id";
    public static final String EXTRA_FOLDER_NAME = "extra_folder_name";
    private static final String PREF_FOLDER_TAGS = "smartflash_folder_tags";

    private ActivityFolderDetailBinding binding;
    private FolderApiService folderApiService;
    private FlashcardSetApiService flashcardSetApiService;

    private long folderId = -1L;
    private FolderDetailDto currentFolder;
    private LibrarySetAdapter setAdapter;
    private final List<String> customTags = new ArrayList<>();
    private String activeTag = "Tất cả";

    private ActivityResultLauncher<Intent> createSetLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFolderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ThemeManager.applyStatusBarColor(getWindow(), this, R.color.sf_bg_screen);

        folderId = getIntent().getLongExtra(EXTRA_FOLDER_ID, -1L);
        String initialName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);
        if (!TextUtils.isEmpty(initialName)) {
            binding.tvFolderTitle.setText(initialName);
        }

        if (folderId <= 0L) {
            Toast.makeText(this, "Không tìm thấy thông tin thư mục.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        folderApiService = RetrofitClient.getInstance(this).getFolderApi();
        flashcardSetApiService = RetrofitClient.getInstance(this).getFlashcardSetApi();

        createSetLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadFolderDetail()
        );

        setupRecyclerView();
        setupListeners();
        loadSavedTags();
        renderTagPills();
        loadFolderDetail();
    }

    private void setupRecyclerView() {
        UserSummaryDto user = TokenManager.getInstance(this).getUser();
        Long currentUserId = user != null ? user.getId() : null;
        setAdapter = new LibrarySetAdapter(currentUserId, set -> {
            if (set.getId() == null) return;
            String[] options = {"Mở học phần", "Chỉnh sửa học phần", "Gỡ khỏi thư mục"};
            new MaterialAlertDialogBuilder(this)
                    .setTitle(set.getTitle())
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            Intent intent = new Intent(this, SetDetailActivity.class);
                            intent.putExtra(SetDetailActivity.EXTRA_SET_ID, set.getId());
                            startActivity(intent);
                        } else if (which == 1) {
                            Intent intent = new Intent(this, CreateEditSetActivity.class);
                            intent.putExtra(CreateEditSetActivity.EXTRA_SET_ID, set.getId());
                            createSetLauncher.launch(intent);
                        } else {
                            removeSetFromFolder(set.getId());
                        }
                    })
                    .show();
        });
        binding.rvFolderSets.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFolderSets.setAdapter(setAdapter);
    }

    private void setupListeners() {
        binding.btnFolderBack.setOnClickListener(v -> finish());
        binding.btnFolderTopAdd.setOnClickListener(v -> openAddStudyMaterialsDialog());
        binding.btnFolderAddMaterials.setOnClickListener(v -> openAddStudyMaterialsDialog());
        binding.btnFolderMore.setOnClickListener(v -> showFolderMoreBottomSheet());
        binding.btnFolderGenerateAi.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng Tạo bằng AI sẽ mở ở Phase 4 (T4.4).", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadFolderDetail() {
        binding.pbFolderLoading.setVisibility(View.VISIBLE);
        folderApiService.getFolderById(folderId).enqueue(new Callback<ApiResponse<FolderDetailDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<FolderDetailDto>> call,
                    @NonNull Response<ApiResponse<FolderDetailDto>> response
            ) {
                binding.pbFolderLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentFolder = response.body().getData();
                    binding.tvFolderTitle.setText(
                            !TextUtils.isEmpty(currentFolder.getName()) ? currentFolder.getName() : "Thư mục"
                    );
                    List<SetSummaryDto> sets = currentFolder.getSets();
                    if (sets == null || sets.isEmpty()) {
                        binding.cardFolderEmpty.setVisibility(View.VISIBLE);
                        binding.rvFolderSets.setVisibility(View.GONE);
                    } else {
                        binding.cardFolderEmpty.setVisibility(View.GONE);
                        binding.rvFolderSets.setVisibility(View.VISIBLE);
                        setAdapter.submitSets(sets);
                    }
                } else {
                    Toast.makeText(FolderDetailActivity.this, "Không thể tải chi tiết thư mục.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FolderDetailDto>> call, @NonNull Throwable t) {
                binding.pbFolderLoading.setVisibility(View.GONE);
                Toast.makeText(FolderDetailActivity.this, "Lỗi kết nối máy chủ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeSetFromFolder(long setId) {
        folderApiService.removeSetFromFolder(folderId, setId).enqueue(new Callback<ApiResponse<FolderDetailDto>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<FolderDetailDto>> call,
                    @NonNull Response<ApiResponse<FolderDetailDto>> response
            ) {
                if (response.isSuccessful()) {
                    Toast.makeText(FolderDetailActivity.this, "Đã gỡ học phần khỏi thư mục", Toast.LENGTH_SHORT).show();
                    loadFolderDetail();
                } else {
                    Toast.makeText(FolderDetailActivity.this, "Không thể gỡ học phần.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FolderDetailDto>> call, @NonNull Throwable t) {
                Toast.makeText(FolderDetailActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== TAG PILLS & NEW TAG DIALOG (Image 6) ====================

    private void loadSavedTags() {
        customTags.clear();
        SharedPreferences prefs = getSharedPreferences(PREF_FOLDER_TAGS, MODE_PRIVATE);
        String raw = prefs.getString("tags_" + folderId, "");
        if (!TextUtils.isEmpty(raw)) {
            for (String t : raw.split("\\|\\|")) {
                if (!t.trim().isEmpty()) {
                    customTags.add(t.trim());
                }
            }
        }
    }

    private void saveTag(String tag) {
        if (TextUtils.isEmpty(tag) || customTags.contains(tag)) return;
        customTags.add(tag);
        SharedPreferences prefs = getSharedPreferences(PREF_FOLDER_TAGS, MODE_PRIVATE);
        prefs.edit().putString("tags_" + folderId, TextUtils.join("||", customTags)).apply();
        activeTag = tag;
        renderTagPills();
    }

    private void renderTagPills() {
        binding.llFolderTagsRow.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int height = Math.round(38 * density);
        int marginEnd = Math.round(8 * density);
        int padH = Math.round(16 * density);

        List<String> pills = new ArrayList<>();
        pills.add("Tất cả");
        pills.addAll(customTags);

        for (String label : pills) {
            boolean selected = label.equals(activeTag);
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, height
            );
            lp.setMarginEnd(marginEnd);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(padH, 0, padH, 0);
            tv.setText(label);
            tv.setTextSize(14f);
            tv.setBackgroundResource(selected ? R.drawable.bg_library_pill_active : R.drawable.bg_library_pill_inactive);
            tv.setTextColor(ContextCompat.getColor(this, selected ? R.color.sf_text_primary : R.color.sf_text_secondary));
            tv.setOnClickListener(v -> {
                activeTag = label;
                renderTagPills();
            });
            binding.llFolderTagsRow.addView(tv);
        }

        // "+ Thẻ mới" chip at end
        TextView newTagChip = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, height
        );
        newTagChip.setLayoutParams(lp);
        newTagChip.setGravity(Gravity.CENTER);
        newTagChip.setPadding(padH, 0, padH, 0);
        newTagChip.setText("+ Thẻ mới");
        newTagChip.setTextSize(14f);
        newTagChip.setBackgroundResource(R.drawable.bg_library_pill_inactive);
        newTagChip.setTextColor(ContextCompat.getColor(this, R.color.sf_text_secondary));
        newTagChip.setOnClickListener(v -> openNewTagDialog());
        binding.llFolderTagsRow.addView(newTagChip);
    }

    private void openNewTagDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_SmartFlash);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_folder_new_tag, null);
        dialog.setContentView(view);
        ThemeManager.applyStatusBarColor(dialog.getWindow(), this, R.color.sf_bg_screen);

        ImageButton btnClose = view.findViewById(R.id.btn_tag_close);
        ImageButton btnConfirm = view.findViewById(R.id.btn_tag_confirm);
        EditText etTagName = view.findViewById(R.id.et_tag_name);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        etTagName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && !s.toString().trim().isEmpty();
                btnConfirm.setEnabled(hasText);
                btnConfirm.setAlpha(hasText ? 1.0f : 0.35f);
                btnConfirm.setColorFilter(ContextCompat.getColor(
                        FolderDetailActivity.this,
                        hasText ? R.color.quizlet_blue : R.color.sf_text_secondary
                ));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String tag = etTagName.getText() != null ? etTagName.getText().toString().trim() : "";
            if (!tag.isEmpty()) {
                saveTag(tag);
                dialog.dismiss();
            }
        });

        int[] chipIds = {R.id.chip_tag_1, R.id.chip_tag_2, R.id.chip_tag_3,
                R.id.chip_tag_4, R.id.chip_tag_5, R.id.chip_tag_6};
        for (int id : chipIds) {
            TextView chip = view.findViewById(id);
            chip.setOnClickListener(v -> {
                String text = chip.getText().toString();
                etTagName.setText(text);
                etTagName.setSelection(text.length());
            });
        }

        dialog.show();
    }

    // ==================== ADD STUDY MATERIALS DIALOG (Images 2, 3, 4, 5) ====================

    private void openAddStudyMaterialsDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_SmartFlash);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_study_materials, null);
        dialog.setContentView(view);
        ThemeManager.applyStatusBarColor(dialog.getWindow(), this, R.color.sf_bg_screen);

        View headerNormal = view.findViewById(R.id.ll_add_mat_header_normal);
        View headerSearch = view.findViewById(R.id.ll_add_mat_header_search);
        ImageButton btnClose = view.findViewById(R.id.btn_add_mat_close);
        ImageButton btnSearchToggle = view.findViewById(R.id.btn_add_mat_search_toggle);
        ImageButton btnSearchBack = view.findViewById(R.id.btn_add_mat_search_back);
        EditText etSearch = view.findViewById(R.id.et_add_mat_search);
        RecyclerView rvSets = view.findViewById(R.id.rv_add_materials_sets);
        ProgressBar pbLoading = view.findViewById(R.id.pb_add_mat_loading);
        TextView tvEmpty = view.findViewById(R.id.tv_add_mat_empty);
        MaterialButton btnBottom = view.findViewById(R.id.btn_add_mat_bottom);

        AddMaterialSetAdapter adapter = new AddMaterialSetAdapter(count -> {
            if (count > 0) {
                btnBottom.setText("Thêm " + count + " mục");
                btnBottom.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.quizlet_blue))
                );
                btnBottom.setTextColor(Color.WHITE);
            } else {
                btnBottom.setText("+ Tạo mới");
                btnBottom.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sf_bg_surface_alt))
                );
                btnBottom.setTextColor(ContextCompat.getColor(this, R.color.sf_text_primary));
            }
        });

        rvSets.setLayoutManager(new LinearLayoutManager(this));
        rvSets.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSearchToggle.setOnClickListener(v -> {
            headerNormal.setVisibility(View.GONE);
            headerSearch.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
        });
        btnSearchBack.setOnClickListener(v -> {
            etSearch.setText("");
            headerSearch.setVisibility(View.GONE);
            headerNormal.setVisibility(View.VISIBLE);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnBottom.setOnClickListener(v -> {
            List<Long> selected = adapter.getSelectedIds();
            if (selected.isEmpty()) {
                showCreateNewMaterialBottomSheet(dialog);
            } else {
                addSelectedSetsToFolder(selected, dialog, btnBottom);
            }
        });

        pbLoading.setVisibility(View.VISIBLE);
        flashcardSetApiService.getMySets(0, 50).enqueue(new Callback<ApiResponse<PageResponseDto<SetSummaryDto>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                    @NonNull Response<ApiResponse<PageResponseDto<SetSummaryDto>>> response
            ) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<SetSummaryDto> sets = response.body().getData().getContent();
                    Set<Long> alreadyInFolder = new HashSet<>();
                    if (currentFolder != null && currentFolder.getSets() != null) {
                        for (SetSummaryDto s : currentFolder.getSets()) {
                            if (s.getId() != null) alreadyInFolder.add(s.getId());
                        }
                    }
                    List<SetSummaryDto> available = new ArrayList<>();
                    if (sets != null) {
                        for (SetSummaryDto s : sets) {
                            if (s.getId() != null && !alreadyInFolder.contains(s.getId())) {
                                available.add(s);
                            }
                        }
                    }
                    adapter.submitSets(available);
                    tvEmpty.setVisibility(available.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                    @NonNull Throwable t
            ) {
                pbLoading.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        dialog.show();
    }

    private void addSelectedSetsToFolder(List<Long> setIds, Dialog parentDialog, MaterialButton btnBottom) {
        btnBottom.setEnabled(false);
        btnBottom.setText("Đang thêm...");
        AtomicInteger remaining = new AtomicInteger(setIds.size());
        AtomicInteger successCount = new AtomicInteger(0);

        for (Long setId : setIds) {
            folderApiService.addSetToFolder(folderId, setId).enqueue(new Callback<ApiResponse<FolderDetailDto>>() {
                @Override
                public void onResponse(
                        @NonNull Call<ApiResponse<FolderDetailDto>> call,
                        @NonNull Response<ApiResponse<FolderDetailDto>> response
                ) {
                    if (response.isSuccessful()) {
                        successCount.incrementAndGet();
                    }
                    checkBatchDone();
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<FolderDetailDto>> call, @NonNull Throwable t) {
                    checkBatchDone();
                }

                private void checkBatchDone() {
                    if (remaining.decrementAndGet() == 0) {
                        parentDialog.dismiss();
                        Toast.makeText(
                                FolderDetailActivity.this,
                                "Đã thêm " + successCount.get() + " học phần vào thư mục",
                                Toast.LENGTH_SHORT
                        ).show();
                        loadFolderDetail();
                    }
                }
            });
        }
    }

    private void showCreateNewMaterialBottomSheet(Dialog parentDialog) {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_add_material_create_new, null);
        sheetDialog.setContentView(sheet);
        sheetDialog.setOnShowListener(d -> {
            View bs = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) bs.setBackgroundColor(Color.TRANSPARENT);
        });

        sheet.findViewById(R.id.row_create_new_set).setOnClickListener(v -> {
            sheetDialog.dismiss();
            parentDialog.dismiss();
            createSetLauncher.launch(new Intent(this, CreateEditSetActivity.class));
        });

        sheet.findViewById(R.id.row_create_new_practice_test).setOnClickListener(v -> {
            sheetDialog.dismiss();
            Toast.makeText(this, "Tính năng Bài kiểm tra thử sẽ mở ở Phase 6 (T6.8).", Toast.LENGTH_SHORT).show();
        });

        sheetDialog.show();
    }

    // ==================== 3-DOT FOLDER MORE BOTTOM SHEET (Image 7) ====================

    private void showFolderMoreBottomSheet() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_folder_more, null);
        sheetDialog.setContentView(sheet);
        sheetDialog.setOnShowListener(d -> {
            View bs = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) bs.setBackgroundColor(Color.TRANSPARENT);
        });

        sheet.findViewById(R.id.row_folder_more_add).setOnClickListener(v -> {
            sheetDialog.dismiss();
            openAddStudyMaterialsDialog();
        });

        sheet.findViewById(R.id.row_folder_more_tag).setOnClickListener(v -> {
            sheetDialog.dismiss();
            openNewTagDialog();
        });

        sheet.findViewById(R.id.row_folder_more_edit).setOnClickListener(v -> {
            sheetDialog.dismiss();
            showEditFolderDialog();
        });

        sheet.findViewById(R.id.row_folder_more_share).setOnClickListener(v -> {
            sheetDialog.dismiss();
            String folderName = binding.tvFolderTitle.getText().toString();
            String token = currentFolder != null && !TextUtils.isEmpty(currentFolder.getShareToken())
                    ? currentFolder.getShareToken()
                    : String.valueOf(folderId);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, "Xem thư mục \"" + folderName + "\" trên ELearner (Mã: " + token + ")");
            startActivity(Intent.createChooser(share, "Chia sẻ thư mục"));
        });

        sheet.findViewById(R.id.row_folder_more_delete).setOnClickListener(v -> {
            sheetDialog.dismiss();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xóa thư mục?")
                    .setMessage("Thư mục sẽ bị xóa, các học phần bên trong vẫn được giữ lại.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xóa", (d, w) -> deleteFolder())
                    .show();
        });

        sheetDialog.show();
    }

    private void showEditFolderDialog() {
        EditText input = new EditText(this);
        input.setText(binding.tvFolderTitle.getText().toString());
        input.setSelection(input.getText().length());
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đổi tên thư mục")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    FolderRequestDto req = new FolderRequestDto(
                            newName,
                            currentFolder != null ? currentFolder.getDescription() : null,
                            currentFolder != null && !TextUtils.isEmpty(currentFolder.getVisibility())
                                    ? currentFolder.getVisibility()
                                    : "PUBLIC"
                    );
                    folderApiService.updateFolder(folderId, req).enqueue(new Callback<ApiResponse<FolderSummaryDto>>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<ApiResponse<FolderSummaryDto>> call,
                                @NonNull Response<ApiResponse<FolderSummaryDto>> response
                        ) {
                            if (response.isSuccessful()) {
                                binding.tvFolderTitle.setText(newName);
                                Toast.makeText(FolderDetailActivity.this, "Đã cập nhật tên thư mục", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<FolderSummaryDto>> call, @NonNull Throwable t) {
                            Toast.makeText(FolderDetailActivity.this, "Không thể cập nhật.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void deleteFolder() {
        folderApiService.deleteFolder(folderId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FolderDetailActivity.this, "Đã xóa thư mục", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(FolderDetailActivity.this, "Không thể xóa thư mục.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(FolderDetailActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
