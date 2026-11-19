package com.elearning.smartflash.ui.folder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.FolderRequestDto;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;
import com.elearning.smartflash.databinding.ActivityCreateFolderBinding;
import com.elearning.smartflash.util.ThemeManager;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Minimalist Quizlet-style Create Folder screen.
 */
public class CreateFolderActivity extends AppCompatActivity {

    public static final String RESULT_FOLDER_ID = "result_folder_id";

    private ActivityCreateFolderBinding binding;
    private boolean saving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();
        binding = ActivityCreateFolderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();

        binding.etFolderName.requestFocus();
        binding.etFolderName.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etFolderName, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void applyStatusBar() {
        Window window = getWindow();
        if (window == null) return;
        boolean nightMode = ThemeManager.isNightMode(this);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_surface));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.sf_bg_surface));
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(!nightMode);
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> finish());

        binding.etFolderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && !s.toString().trim().isEmpty();
                binding.btnSave.setEnabled(hasText && !saving);
                binding.btnSave.setAlpha(hasText ? 1.0f : 0.35f);
                binding.btnSave.setColorFilter(ContextCompat.getColor(
                        CreateFolderActivity.this,
                        hasText ? R.color.quizlet_blue : R.color.sf_text_secondary
                ));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.etFolderName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCreateFolder();
                return true;
            }
            return false;
        });

        binding.btnSave.setOnClickListener(v -> submitCreateFolder());
    }

    private void submitCreateFolder() {
        if (saving) return;
        String name = binding.etFolderName.getText() != null
                ? binding.etFolderName.getText().toString().trim()
                : "";
        if (name.isEmpty()) {
            return;
        }

        setSavingState(true);
        FolderRequestDto request = new FolderRequestDto(name, null, "PUBLIC");
        RetrofitClient.getInstance(this).getFolderApi().createFolder(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<FolderSummaryDto>> call,
                                   @NonNull Response<ApiResponse<FolderSummaryDto>> response) {
                setSavingState(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CreateFolderActivity.this, "Đã tạo thư mục", Toast.LENGTH_SHORT).show();
                    FolderSummaryDto created = response.body().getData();
                    Intent result = new Intent();
                    if (created != null && created.getId() != null) {
                        result.putExtra(RESULT_FOLDER_ID, created.getId());
                    }
                    setResult(RESULT_OK, result);
                    if (created != null && created.getId() != null) {
                        Intent detailIntent = new Intent(CreateFolderActivity.this, FolderDetailActivity.class);
                        detailIntent.putExtra(FolderDetailActivity.EXTRA_FOLDER_ID, created.getId());
                        detailIntent.putExtra(FolderDetailActivity.EXTRA_FOLDER_NAME, created.getName());
                        startActivity(detailIntent);
                    }
                    finish();
                } else {
                    Toast.makeText(CreateFolderActivity.this, parseErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FolderSummaryDto>> call, @NonNull Throwable t) {
                setSavingState(false);
                Toast.makeText(CreateFolderActivity.this,
                        t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSavingState(boolean busy) {
        saving = busy;
        binding.btnSave.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        binding.btnSave.setEnabled(!busy);
        binding.progressSaving.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                JSONObject json = new JSONObject(response.errorBody().string());
                if (json.has("message")) {
                    return json.getString("message");
                }
            }
        } catch (Exception ignored) {
        }
        return "Không thể tạo thư mục (mã " + response.code() + ")";
    }
}
