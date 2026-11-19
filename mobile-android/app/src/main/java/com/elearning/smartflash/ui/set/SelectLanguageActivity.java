package com.elearning.smartflash.ui.set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivitySelectLanguageBinding;
import com.elearning.smartflash.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen Quizlet-style language picker with TOP LANGUAGES section and search bar.
 */
public class SelectLanguageActivity extends AppCompatActivity {

    public static final String EXTRA_HEADER_TITLE = "extra_header_title";
    public static final String EXTRA_TARGET = "extra_target";
    public static final String EXTRA_CURRENT_CODE = "extra_current_code";
    public static final String EXTRA_SELECTED_CODE = "extra_selected_code";

    private ActivitySelectLanguageBinding binding;
    private LanguageAdapter adapter;
    private final List<RowItem> allRows = new ArrayList<>();
    private String currentCode;
    private String target;
    private boolean searchVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyStatusBar();
        binding = ActivitySelectLanguageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String headerTitle = getIntent().getStringExtra(EXTRA_HEADER_TITLE);
        target = getIntent().getStringExtra(EXTRA_TARGET);
        currentCode = getIntent().getStringExtra(EXTRA_CURRENT_CODE);

        if (headerTitle != null && !headerTitle.isEmpty()) {
            binding.tvHeaderTitle.setText(headerTitle.toUpperCase(Locale.ROOT));
        }

        buildLanguageRows();

        adapter = new LanguageAdapter(allRows, currentCode, option -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_SELECTED_CODE, option.code.isEmpty() ? null : option.code);
            data.putExtra(EXTRA_TARGET, target);
            setResult(RESULT_OK, data);
            finish();
        });

        binding.rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLanguages.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> {
            if (searchVisible) {
                toggleSearch(false);
            } else {
                finish();
            }
        });

        binding.btnSearch.setOnClickListener(v -> toggleSearch(!searchVisible));

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLanguages(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
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

    private void toggleSearch(boolean show) {
        searchVisible = show;
        binding.tvHeaderTitle.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.etSearch.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        } else {
            binding.etSearch.setText("");
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
            }
            adapter.updateRows(allRows);
        }
    }

    private void filterLanguages(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            adapter.updateRows(allRows);
            return;
        }
        List<RowItem> filtered = new ArrayList<>();
        for (RowItem row : allRows) {
            if (!row.isHeader && row.label.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(row);
            }
        }
        adapter.updateRows(filtered);
    }

    private void buildLanguageRows() {
        allRows.clear();
        allRows.add(RowItem.header("TOP LANGUAGES"));
        for (SetLanguageOptions.LanguageEntry entry : SetLanguageOptions.TOP_LANGUAGES) {
            allRows.add(RowItem.option(entry.code, entry.label));
        }
        allRows.add(RowItem.header("ALL LANGUAGES"));
        for (SetLanguageOptions.LanguageEntry entry : SetLanguageOptions.OTHER_LANGUAGES) {
            allRows.add(RowItem.option(entry.code, entry.label));
        }
    }

    private static class RowItem {
        final boolean isHeader;
        final String code;
        final String label;

        private RowItem(boolean isHeader, String code, String label) {
            this.isHeader = isHeader;
            this.code = code;
            this.label = label;
        }

        static RowItem header(String title) {
            return new RowItem(true, "", title);
        }

        static RowItem option(String code, String label) {
            return new RowItem(false, code, label);
        }
    }

    private static class LanguageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_OPTION = 1;

        interface OnOptionClick {
            void onClick(RowItem item);
        }

        private List<RowItem> rows;
        private final String currentCode;
        private final OnOptionClick listener;

        LanguageAdapter(List<RowItem> rows, String currentCode, OnOptionClick listener) {
            this.rows = new ArrayList<>(rows);
            this.currentCode = currentCode;
            this.listener = listener;
        }

        void updateRows(List<RowItem> newRows) {
            this.rows = new ArrayList<>(newRows);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).isHeader ? TYPE_HEADER : TYPE_OPTION;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderVH(inflater.inflate(R.layout.item_language_header, parent, false));
            }
            return new OptionVH(inflater.inflate(R.layout.item_language_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem item = rows.get(position);
            if (holder instanceof HeaderVH headerVH) {
                headerVH.tvHeader.setText(item.label);
            } else if (holder instanceof OptionVH optionVH) {
                optionVH.tvName.setText(item.label);
                boolean selected = currentCode != null && !currentCode.isEmpty()
                        && currentCode.equalsIgnoreCase(item.code);
                optionVH.ivCheck.setVisibility(selected ? View.VISIBLE : View.GONE);
                optionVH.itemView.setOnClickListener(v -> listener.onClick(item));
            }
        }

        static class HeaderVH extends RecyclerView.ViewHolder {
            final TextView tvHeader;

            HeaderVH(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_section_header);
            }
        }

        static class OptionVH extends RecyclerView.ViewHolder {
            final TextView tvName;
            final ImageView ivCheck;

            OptionVH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_language_name);
                ivCheck = itemView.findViewById(R.id.iv_selected_check);
            }
        }
    }
}
