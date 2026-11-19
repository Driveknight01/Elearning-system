package com.elearning.smartflash.ui.set;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivitySetOptionsBinding;
import com.elearning.smartflash.util.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Quizlet-style Set Options screen for configuring languages, auto-suggest, privacy, and deletion.
 */
public class SetOptionsActivity extends AppCompatActivity {

    public static final String EXTRA_TERM_LANG = "extra_term_lang";
    public static final String EXTRA_DEF_LANG = "extra_def_lang";
    public static final String EXTRA_AUTO_SUGGEST = "extra_auto_suggest";
    public static final String EXTRA_VISIBILITY = "extra_visibility";
    public static final String EXTRA_EDITABLE_BY = "extra_editable_by";
    public static final String EXTRA_IS_EDIT_MODE = "extra_is_edit_mode";
    public static final String EXTRA_DELETE_SET = "extra_delete_set";

    private static final String[] VISIBILITY_LABELS = {
            "Everyone",
            "People with a link",
            "Just me"
    };
    private static final String[] VISIBILITY_VALUES = {
            "PUBLIC",
            "LINK_ONLY",
            "PRIVATE"
    };

    private static final String[] EDITABLE_LABELS = {
            "Just me",
            "Certain classes",
            "People with a password"
    };
    private static final String[] EDITABLE_VALUES = {
            "JUST_ME",
            "CLASSES",
            "PASSWORD"
    };

    private ActivitySetOptionsBinding binding;
    private String termLang;
    private String defLang;
    private boolean autoSuggest = true;
    private String visibility = "PUBLIC";
    private String editableBy = "JUST_ME";
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> languageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String target = result.getData().getStringExtra(SelectLanguageActivity.EXTRA_TARGET);
                String selectedCode = result.getData().getStringExtra(SelectLanguageActivity.EXTRA_SELECTED_CODE);
                if ("term".equals(target)) {
                    termLang = selectedCode;
                } else if ("def".equals(target)) {
                    defLang = selectedCode;
                }
                renderValues();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();
        binding = ActivitySetOptionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        termLang = intent.getStringExtra(EXTRA_TERM_LANG);
        defLang = intent.getStringExtra(EXTRA_DEF_LANG);
        autoSuggest = intent.getBooleanExtra(EXTRA_AUTO_SUGGEST, true);
        String visExtra = intent.getStringExtra(EXTRA_VISIBILITY);
        if (visExtra != null && !visExtra.isEmpty()) {
            visibility = visExtra;
        }
        String editExtra = intent.getStringExtra(EXTRA_EDITABLE_BY);
        if (editExtra != null && !editExtra.isEmpty()) {
            editableBy = editExtra;
        }
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false);

        renderValues();
        setupListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithResult(false);
            }
        });
    }

    private void applyStatusBar() {
        Window window = getWindow();
        if (window == null) return;
        boolean nightMode = ThemeManager.isNightMode(this);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_surface));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.sf_bg_screen));
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(!nightMode);
    }

    private void renderValues() {
        binding.tvTermsLanguage.setText(SetLanguageOptions.getDisplayName(termLang, "Select language"));
        binding.tvDefinitionsLanguage.setText(SetLanguageOptions.getDisplayName(defLang, "Select language"));
        binding.switchAutoSuggest.setChecked(autoSuggest);
        binding.tvVisibleTo.setText(getLabelForValue(VISIBILITY_VALUES, VISIBILITY_LABELS, visibility, "Everyone"));
        binding.tvEditableBy.setText(getLabelForValue(EDITABLE_VALUES, EDITABLE_LABELS, editableBy, "Just me"));
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finishWithResult(false));

        binding.rowTermsLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectLanguageActivity.class);
            intent.putExtra(SelectLanguageActivity.EXTRA_HEADER_TITLE, "TERM LANGUAGE");
            intent.putExtra(SelectLanguageActivity.EXTRA_TARGET, "term");
            intent.putExtra(SelectLanguageActivity.EXTRA_CURRENT_CODE, termLang);
            languageLauncher.launch(intent);
        });

        binding.rowDefinitionsLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectLanguageActivity.class);
            intent.putExtra(SelectLanguageActivity.EXTRA_HEADER_TITLE, "DEFINITION LANGUAGE");
            intent.putExtra(SelectLanguageActivity.EXTRA_TARGET, "def");
            intent.putExtra(SelectLanguageActivity.EXTRA_CURRENT_CODE, defLang);
            languageLauncher.launch(intent);
        });

        binding.switchAutoSuggest.setOnCheckedChangeListener((buttonView, isChecked) ->
                autoSuggest = isChecked);

        binding.rowVisibleTo.setOnClickListener(v -> showChoiceDialog(
                "Visible to",
                VISIBILITY_LABELS,
                VISIBILITY_VALUES,
                visibility,
                selected -> {
                    visibility = selected;
                    renderValues();
                }));

        binding.rowEditableBy.setOnClickListener(v -> showChoiceDialog(
                "Editable by",
                EDITABLE_LABELS,
                EDITABLE_VALUES,
                editableBy,
                selected -> {
                    editableBy = selected;
                    renderValues();
                }));

        binding.rowDeleteSet.setOnClickListener(v -> confirmDeleteSet());
    }

    private interface OnValueSelected {
        void onSelected(String value);
    }

    private void showChoiceDialog(String title, String[] labels, String[] values,
                                  String currentValue, OnValueSelected listener) {
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(currentValue)) {
                checked = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    listener.onSelected(values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteSet() {
        String title = isEditMode ? "Delete this set?" : "Discard this set?";
        String message = isEditMode
                ? "This flashcard set will be permanently deleted."
                : "Are you sure you want to discard this unsaved flashcard set?";
        String positiveText = isEditMode ? "Delete" : "Discard";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(positiveText, (dialog, which) -> finishWithResult(true))
                .show();
    }

    private void finishWithResult(boolean deleteSet) {
        Intent data = new Intent();
        data.putExtra(EXTRA_TERM_LANG, termLang);
        data.putExtra(EXTRA_DEF_LANG, defLang);
        data.putExtra(EXTRA_AUTO_SUGGEST, autoSuggest);
        data.putExtra(EXTRA_VISIBILITY, visibility);
        data.putExtra(EXTRA_EDITABLE_BY, editableBy);
        data.putExtra(EXTRA_DELETE_SET, deleteSet);
        setResult(RESULT_OK, data);
        finish();
    }

    private static String getLabelForValue(String[] values, String[] labels, String target, String fallback) {
        if (target == null) return fallback;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(target)) {
                return labels[i];
            }
        }
        return fallback;
    }
}
