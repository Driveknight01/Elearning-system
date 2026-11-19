package com.elearning.smartflash.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivityMainBinding;
import com.elearning.smartflash.util.ThemeManager;
import com.elearning.smartflash.ui.chat.AiChatPlaceholderFragment;
import com.elearning.smartflash.ui.folder.CreateFolderActivity;
import com.elearning.smartflash.ui.home.HomeFragment;
import com.elearning.smartflash.ui.library.LibraryFragment;
import com.elearning.smartflash.ui.profile.ProfileFragment;
import com.elearning.smartflash.ui.search.SearchFragment;
import com.elearning.smartflash.ui.set.CreateEditSetActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Main host with 4-button Quizlet-style BottomNavigationView:
 * Home, Create (+), Library, and AI Chatbot.
 * Top-bar Search and Avatar actions route to SearchFragment and ProfileFragment.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab_id";
    private static final String TAG_HOME = "tab_home";
    private static final String TAG_LIBRARY = "tab_library";
    private static final String TAG_AI_CHAT = "tab_ai_chat";
    private static final String TAG_SEARCH = "page_search";
    private static final String TAG_PROFILE = "page_profile";

    private ActivityMainBinding binding;
    private int currentTabId = R.id.nav_home;
    private boolean auxiliaryFragmentVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyWhiteStatusBar();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        }

        setupBottomNavigation();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.bottomNav.getVisibility() != View.VISIBLE) {
                    switchToTab(currentTabId);
                    binding.bottomNav.setSelectedItemId(currentTabId);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        if (savedInstanceState == null) {
            switchToTab(currentTabId);
        } else {
            binding.bottomNav.setSelectedItemId(currentTabId);
        }
    }

    private void applyWhiteStatusBar() {
        updateStatusBarColor(ContextCompat.getColor(this, R.color.sf_home_top_status));
    }

    private void updateStatusBarColor(int color) {
        Window window = getWindow();
        if (window == null) return;
        boolean nightMode = ThemeManager.isNightMode(this);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.sf_bg_screen));
        androidx.core.view.WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!nightMode);
        controller.setAppearanceLightNavigationBars(!nightMode);
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_create) {
                showCreateBottomSheet();
                return false;
            }
            switchToTab(itemId);
            return true;
        });
    }

    public void navigateToTab(@IdRes int tabId) {
        if (binding != null && tabId != R.id.nav_create) {
            if (binding.bottomNav.getSelectedItemId() == tabId) {
                switchToTab(tabId);
            } else {
                binding.bottomNav.setSelectedItemId(tabId);
            }
        }
    }

    public void openSearch() {
        showAuxiliaryFragment(TAG_SEARCH, new SearchFragment());
    }

    public void openProfile() {
        showAuxiliaryFragment(TAG_PROFILE, new ProfileFragment());
    }

    public void showCreateBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_create_chooser, null);
        sheetView.findViewById(R.id.card_create_set).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CreateEditSetActivity.class));
        });
        sheetView.findViewById(R.id.card_create_folder).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, CreateFolderActivity.class));
        });
        dialog.setContentView(sheetView);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            }
        });
        dialog.show();
    }

    public void updateHomeScrollStatusBar(int color) {
        if (currentTabId == R.id.nav_home && !auxiliaryFragmentVisible) {
            updateStatusBarColor(color);
        }
    }

    private void switchToTab(@IdRes int tabId) {
        boolean returningFromAuxiliary = auxiliaryFragmentVisible;
        currentTabId = tabId;
        auxiliaryFragmentVisible = false;
        binding.navDivider.setVisibility(View.VISIBLE);
        binding.bottomNav.setVisibility(View.VISIBLE);
        String targetTag = getTagForTab(tabId);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setCustomAnimations(R.anim.slide_in_right_fade, R.anim.fade_out_fast);
        if (returningFromAuxiliary) {
            tx.setCustomAnimations(R.anim.fade_in_fast, R.anim.slide_out_right_fade);
        } else {
            tx.setCustomAnimations(R.anim.fade_in_fast, R.anim.fade_out_fast);
        }

        for (Fragment existing : fm.getFragments()) {
            if (existing != null && existing.isAdded()) {
                tx.hide(existing);
            }
        }

        Fragment target = fm.findFragmentByTag(targetTag);
        if (target == null) {
            target = createFragmentForTab(tabId);
            tx.add(R.id.fragment_container, target, targetTag);
        } else {
            tx.show(target);
        }
        tx.commit();

        if (tabId == R.id.nav_home) {
            if (target instanceof HomeFragment homeFragment && homeFragment.isAdded()) {
                homeFragment.syncStatusBarColor();
            } else {
                updateStatusBarColor(ContextCompat.getColor(this, R.color.sf_home_top_status));
            }
        } else {
            updateStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_screen));
        }
    }

    private void showAuxiliaryFragment(String tag, Fragment newInstance) {
        auxiliaryFragmentVisible = true;
        binding.navDivider.setVisibility(View.GONE);
        binding.bottomNav.setVisibility(View.GONE);
        updateStatusBarColor(ContextCompat.getColor(this, R.color.sf_bg_screen));
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setCustomAnimations(R.anim.slide_in_right_fade, R.anim.fade_out_fast);

        for (Fragment existing : fm.getFragments()) {
            if (existing != null && existing.isAdded()) {
                tx.hide(existing);
            }
        }

        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            target = newInstance;
            tx.add(R.id.fragment_container, target, tag);
        } else {
            tx.show(target);
        }
        tx.commit();
    }

    private String getTagForTab(@IdRes int tabId) {
        if (tabId == R.id.nav_library) {
            return TAG_LIBRARY;
        }
        if (tabId == R.id.nav_ai_chat) {
            return TAG_AI_CHAT;
        }
        return TAG_HOME;
    }

    private Fragment createFragmentForTab(@IdRes int tabId) {
        if (tabId == R.id.nav_library) {
            return new LibraryFragment();
        }
        if (tabId == R.id.nav_ai_chat) {
            return new AiChatPlaceholderFragment();
        }
        return new HomeFragment();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, currentTabId);
    }
}
