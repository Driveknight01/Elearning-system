package com.elearning.smartflash.ui.main;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.elearning.smartflash.R;
import com.elearning.smartflash.databinding.ActivityMainBinding;
import com.elearning.smartflash.ui.home.HomeFragment;
import com.elearning.smartflash.ui.library.LibraryFragment;
import com.elearning.smartflash.ui.profile.ProfileFragment;
import com.elearning.smartflash.ui.search.SearchFragment;

/**
 * Main application host featuring BottomNavigationView with 4 tabs:
 * Trang chủ (Home), Tìm kiếm (Search), Thư viện (Library), and Cá nhân (Profile).
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab_id";
    private static final String TAG_HOME = "tab_home";
    private static final String TAG_SEARCH = "tab_search";
    private static final String TAG_LIBRARY = "tab_library";
    private static final String TAG_PROFILE = "tab_profile";

    private ActivityMainBinding binding;
    private int currentTabId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
        }

        setupBottomNavigation();
        if (savedInstanceState == null) {
            switchToTab(currentTabId);
        } else {
            binding.bottomNav.setSelectedItemId(currentTabId);
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            switchToTab(itemId);
            return true;
        });
    }

    public void navigateToTab(@IdRes int tabId) {
        if (binding != null && binding.bottomNav.getSelectedItemId() != tabId) {
            binding.bottomNav.setSelectedItemId(tabId);
        }
    }

    private void switchToTab(@IdRes int tabId) {
        currentTabId = tabId;
        String targetTag = getTagForTab(tabId);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

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
    }

    private String getTagForTab(@IdRes int tabId) {
        if (tabId == R.id.nav_search) {
            return TAG_SEARCH;
        }
        if (tabId == R.id.nav_library) {
            return TAG_LIBRARY;
        }
        if (tabId == R.id.nav_profile) {
            return TAG_PROFILE;
        }
        return TAG_HOME;
    }

    private Fragment createFragmentForTab(@IdRes int tabId) {
        if (tabId == R.id.nav_search) {
            return new SearchFragment();
        }
        if (tabId == R.id.nav_library) {
            return new LibraryFragment();
        }
        if (tabId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, currentTabId);
    }
}
