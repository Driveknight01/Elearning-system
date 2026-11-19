package com.elearning.smartflash.ui.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.remote.api.FlashcardSetApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.api.UserApiService;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;
import com.elearning.smartflash.data.remote.dto.ProfilePrivacyConfigDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;
import com.elearning.smartflash.data.remote.dto.UserSearchItemDto;
import com.elearning.smartflash.databinding.FragmentSearchBinding;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.ui.profile.ProfileViewModel;
import com.elearning.smartflash.ui.profile.PublicProfileDialogHelper;
import com.elearning.smartflash.ui.set.SetDetailActivity;
import com.elearning.smartflash.ui.search.SearchResultModels.ClassItem;
import com.elearning.smartflash.ui.search.SearchResultModels.QuestionItem;
import com.elearning.smartflash.ui.search.SearchResultModels.SetItem;
import com.elearning.smartflash.ui.search.SearchResultModels.UserItem;
import com.elearning.smartflash.util.ProfileUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search tab supporting 7-tab Quizlet-style search results,
 * Sets filter bottom sheet, and {@code @username} public profile lookup.
 */
public class SearchFragment extends Fragment {

    private static final String[] SEARCH_TABS = {
            "Tất cả",
            "Học phần",
            "Đề kiểm tra thử",
            "Hướng dẫn học",
            "Câu hỏi",
            "Người dùng",
            "Lớp học"
    };

    private FragmentSearchBinding binding;
    private ProfileViewModel profileViewModel;
    private FlashcardSetApiService flashcardSetApiService;
    private UserApiService userApiService;
    private PublicProfileDto lastLoadedProfile;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
    private int baseBottomPadding;
    private int lastAppliedBottomPadding = -1;

    private int activeTabIndex = 0;
    private int filterTermsMode = 0;   // 0=all, 1=<20, 2=20-49, 3=50+
    private int filterCreatorMode = 0; // 0=all, 1=plus, 2=teacher, 3=verified
    private int filterContentMode = 0; // 0=all, 1=images, 2=diagrams
    private boolean suppressSuggestionWatch = false;

    private SearchSuggestionAdapter suggestionAdapter;
    private SearchSetAdapter allSetsAdapter;
    private SearchQuestionAdapter allQuestionsAdapter;
    private SearchUserAdapter allUsersAdapter;
    private SearchClassAdapter allClassesAdapter;

    private SearchSetAdapter singleSetsAdapter;
    private SearchQuestionAdapter singleQuestionsAdapter;
    private SearchUserAdapter singleUsersAdapter;
    private SearchClassAdapter singleClassesAdapter;

    private final List<SetItem> currentSets = new ArrayList<>();
    private final List<QuestionItem> currentQuestions = new ArrayList<>();
    private final List<UserItem> currentUsers = new ArrayList<>();
    private final List<ClassItem> currentClasses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        flashcardSetApiService = RetrofitClient.getInstance(requireContext()).getFlashcardSetApi();
        userApiService = RetrofitClient.getInstance(requireContext()).getUserApi();

        baseBottomPadding = view.getPaddingBottom();
        keyboardLayoutListener = () -> adjustForKeyboard(view);
        view.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        setupSearchBar();
        setupTabsAndLists();
        focusSearchInput();
    }

    private void setupSearchBar() {
        binding.btnSearchBack.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_home);
            }
        });

        binding.cardSearchGamePromo.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.navigateToTab(R.id.nav_home);
            }
        });

        binding.btnSearchSubmit.setOnClickListener(v -> performLookup());
        binding.btnSearchClear.setOnClickListener(v -> resetToEmptyState());

        binding.etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.length() > 0;
                binding.btnSearchClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
                binding.btnSearchSubmit.setVisibility(hasText ? View.GONE : View.VISIBLE);

                if (suppressSuggestionWatch) {
                    return;
                }
                if (hasText) {
                    showSuggestionsForQuery(s.toString().trim());
                } else {
                    binding.llSearchSuggestions.setVisibility(View.GONE);
                    binding.llSearchResultsContainer.setVisibility(View.GONE);
                    binding.cardSearchProfileResult.setVisibility(View.GONE);
                    binding.tvSearchFeedback.setVisibility(View.GONE);
                    binding.llSearchEmptyState.setVisibility(View.VISIBLE);
                    binding.cardSearchGamePromo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performLookup();
                return true;
            }
            return false;
        });

        binding.btnOpenPublicProfileDialog.setOnClickListener(v -> {
            if (lastLoadedProfile != null) {
                PublicProfileDialogHelper.show(requireContext(), lastLoadedProfile);
            }
        });
    }

    private void setupTabsAndLists() {
        for (String title : SEARCH_TABS) {
            binding.tabLayoutSearch.addTab(binding.tabLayoutSearch.newTab().setText(title));
        }

        binding.tabLayoutSearch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                activeTabIndex = tab.getPosition();
                renderActiveTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        SearchSetAdapter.OnSetActionListener setListener = new SearchSetAdapter.OnSetActionListener() {
            @Override
            public void onPreview(SetItem item) {
                openSetDetail(item);
            }

            @Override
            public void onStudy(SetItem item) {
                openSetDetail(item);
            }
        };

        allSetsAdapter = new SearchSetAdapter(false, setListener);
        allQuestionsAdapter = new SearchQuestionAdapter(item ->
                Toast.makeText(requireContext(), "Xem câu hỏi: " + item.subject, Toast.LENGTH_SHORT).show()
        );
        allUsersAdapter = new SearchUserAdapter(item -> openPublicUserProfile(item.username));
        allClassesAdapter = new SearchClassAdapter(item ->
                Toast.makeText(requireContext(), "Xem lớp học: " + item.title, Toast.LENGTH_SHORT).show()
        );

        singleSetsAdapter = new SearchSetAdapter(true, setListener);
        singleQuestionsAdapter = new SearchQuestionAdapter(item ->
                Toast.makeText(requireContext(), "Xem câu hỏi: " + item.subject, Toast.LENGTH_SHORT).show()
        );
        singleUsersAdapter = new SearchUserAdapter(item -> openPublicUserProfile(item.username));
        singleClassesAdapter = new SearchClassAdapter(item ->
                Toast.makeText(requireContext(), "Xem lớp học: " + item.title, Toast.LENGTH_SHORT).show()
        );

        suggestionAdapter = new SearchSuggestionAdapter(suggestion -> {
            suppressSuggestionWatch = true;
            binding.etSearchQuery.setText(suggestion);
            binding.etSearchQuery.setSelection(suggestion.length());
            suppressSuggestionWatch = false;
            performLookup();
        });
        binding.rvSearchSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchSuggestions.setAdapter(suggestionAdapter);
        binding.btnSuggestionsViewAll.setOnClickListener(v -> performLookup());

        binding.rvAllSets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllSets.setAdapter(allSetsAdapter);

        binding.rvAllQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllQuestions.setAdapter(allQuestionsAdapter);

        binding.rvAllUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllUsers.setAdapter(allUsersAdapter);

        binding.rvAllClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAllClasses.setAdapter(allClassesAdapter);

        binding.rvSearchSingleTab.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.btnViewAllSets.setOnClickListener(v -> selectTab(1));
        binding.btnViewAllQuestions.setOnClickListener(v -> selectTab(4));
        binding.btnViewAllUsers.setOnClickListener(v -> selectTab(5));
        binding.btnViewAllClasses.setOnClickListener(v -> selectTab(6));

        binding.btnSearchSetsFilter.setOnClickListener(v -> showSetsFilterBottomSheet());
    }

    private void selectTab(int index) {
        TabLayout.Tab tab = binding.tabLayoutSearch.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
    }

    private void resetToEmptyState() {
        binding.etSearchQuery.setText("");
        binding.tvSearchFeedback.setVisibility(View.GONE);
        binding.cardSearchProfileResult.setVisibility(View.GONE);
        binding.llSearchResultsContainer.setVisibility(View.GONE);
        binding.llSearchEmptyState.setVisibility(View.VISIBLE);
        binding.cardSearchGamePromo.setVisibility(View.VISIBLE);
        lastLoadedProfile = null;
        focusSearchInput();
    }

    private void performLookup() {
        String raw = binding.etSearchQuery.getText() != null
                ? binding.etSearchQuery.getText().toString().trim()
                : "";

        binding.tvSearchFeedback.setVisibility(View.GONE);
        binding.cardSearchProfileResult.setVisibility(View.GONE);
        binding.llSearchSuggestions.setVisibility(View.GONE);
        lastLoadedProfile = null;

        if (TextUtils.isEmpty(raw)) {
            binding.llSearchResultsContainer.setVisibility(View.GONE);
            binding.llSearchEmptyState.setVisibility(View.VISIBLE);
            binding.cardSearchGamePromo.setVisibility(View.VISIBLE);
            binding.tvSearchFeedback.setText("Vui lòng nhập từ khóa hoặc @username cần tìm.");
            binding.tvSearchFeedback.setVisibility(View.VISIBLE);
            return;
        }

        hideKeyboard();
        binding.llSearchEmptyState.setVisibility(View.GONE);
        binding.cardSearchGamePromo.setVisibility(View.GONE);

        if (raw.startsWith("@")) {
            String username = raw.substring(1).trim();
            if (!TextUtils.isEmpty(username)) {
                lookupPublicProfile(username);
                return;
            }
        }

        searchPublicSetsFromApi(raw);
        searchUsersFromApi(raw);
    }

    private void showSuggestionsForQuery(String query) {
        if (TextUtils.isEmpty(query)) return;
        binding.llSearchEmptyState.setVisibility(View.GONE);
        binding.cardSearchGamePromo.setVisibility(View.GONE);
        binding.llSearchResultsContainer.setVisibility(View.GONE);
        binding.cardSearchProfileResult.setVisibility(View.GONE);
        binding.tvSearchFeedback.setVisibility(View.GONE);
        binding.llSearchSuggestions.setVisibility(View.VISIBLE);

        String lower = query.toLowerCase();
        List<String> list;
        if (lower.startsWith("phr")) {
            list = Arrays.asList(
                    "phrasal verbs",
                    query,
                    "phrasel verbs",
                    "phrasal verbs b2",
                    "phrase",
                    "phrasal"
            );
        } else {
            list = Arrays.asList(
                    query + " cơ bản",
                    query,
                    query + " nâng cao",
                    query + " b2",
                    query + " thông dụng",
                    query + " luyện thi"
            );
        }
        suggestionAdapter.submitSuggestions(query, list);
    }

    private static final String[] AVATAR_PALETTE = {
            "#4255FF", "#EC4899", "#3B82F6", "#10B981", "#8B5CF6", "#F59E0B"
    };

    private void searchPublicSetsFromApi(String query) {
        if (binding == null) return;
        binding.pbSearchLoading.setVisibility(View.VISIBLE);
        binding.llSearchResultsContainer.setVisibility(View.GONE);
        binding.tvSearchFeedback.setVisibility(View.GONE);

        flashcardSetApiService.searchPublicSets(query, null, "newest", 0, 30)
                .enqueue(new Callback<ApiResponse<PageResponseDto<SetSummaryDto>>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                            @NonNull Response<ApiResponse<PageResponseDto<SetSummaryDto>>> response
                    ) {
                        if (binding == null) return;
                        binding.pbSearchLoading.setVisibility(View.GONE);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {
                            List<SetSummaryDto> content = response.body().getData().getContent();
                            bindRealSearchResults(query, content);
                        } else {
                            binding.tvSearchFeedback.setText("Không thể tải kết quả tìm kiếm lúc này.");
                            binding.tvSearchFeedback.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                            @NonNull Throwable t
                    ) {
                        if (binding == null) return;
                        binding.pbSearchLoading.setVisibility(View.GONE);
                        binding.tvSearchFeedback.setText("Lỗi kết nối máy chủ. Vui lòng thử lại.");
                        binding.tvSearchFeedback.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void bindRealSearchResults(String query, List<SetSummaryDto> sets) {
        currentSets.clear();
        if (sets != null) {
            for (SetSummaryDto dto : sets) {
                long id = dto.getId() != null ? dto.getId() : -1L;
                String title = !TextUtils.isEmpty(dto.getTitle()) ? dto.getTitle() : "Chưa đặt tiêu đề";
                int termCount = dto.getCardCount() != null ? dto.getCardCount() : 0;

                String author = "SmartFlash";
                String creatorType = "all";
                if (dto.getOwner() != null) {
                    if (!TextUtils.isEmpty(dto.getOwner().getUsername())) {
                        author = dto.getOwner().getUsername();
                    } else if (!TextUtils.isEmpty(dto.getOwner().getFullName())) {
                        author = dto.getOwner().getFullName();
                    }
                    if (dto.getOwner().isVerifiedTeacher()) {
                        creatorType = "teacher";
                    }
                }

                int paletteIdx = Math.abs(author.hashCode()) % AVATAR_PALETTE.length;
                currentSets.add(new SetItem(
                        id,
                        title,
                        termCount,
                        author,
                        AVATAR_PALETTE[paletteIdx],
                        false,
                        false,
                        creatorType
                ));
            }
        }

        currentQuestions.clear();
        currentUsers.clear();
        currentClasses.clear();

        boolean hasSets = !currentSets.isEmpty();
        boolean hasUsers = !currentUsers.isEmpty();
        binding.llAllSetsHeader.setVisibility(hasSets ? View.VISIBLE : View.GONE);
        binding.rvAllSets.setVisibility(hasSets ? View.VISIBLE : View.GONE);
        binding.llAllQuestionsHeader.setVisibility(View.GONE);
        binding.rvAllQuestions.setVisibility(View.GONE);
        binding.llAllUsersHeader.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
        binding.rvAllUsers.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
        binding.llAllClassesHeader.setVisibility(View.GONE);
        binding.rvAllClasses.setVisibility(View.GONE);

        boolean hasAny = hasSets || hasUsers;
        binding.tvAllResultsEmpty.setVisibility(hasAny ? View.GONE : View.VISIBLE);
        if (!hasAny) {
            binding.tvAllResultsEmpty.setText("Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
        }

        allSetsAdapter.submitList(currentSets.subList(0, Math.min(3, currentSets.size())));
        allQuestionsAdapter.submitList(currentQuestions);
        allUsersAdapter.submitList(currentUsers.subList(0, Math.min(3, currentUsers.size())));
        allClassesAdapter.submitList(currentClasses);

        binding.llSearchResultsContainer.setVisibility(View.VISIBLE);
        selectTab(0);
        renderActiveTab();
    }

    private void searchUsersFromApi(String query) {
        if (binding == null) return;
        userApiService.searchPublicUsers(query, 0, 20)
                .enqueue(new Callback<ApiResponse<List<UserSearchItemDto>>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<List<UserSearchItemDto>>> call,
                            @NonNull Response<ApiResponse<List<UserSearchItemDto>>> response
                    ) {
                        if (binding == null) return;
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {
                            bindUserSearchResults(response.body().getData());
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ApiResponse<List<UserSearchItemDto>>> call,
                            @NonNull Throwable t
                    ) {
                        // Keep sets results intact if user search fails silently
                    }
                });
    }

    private void bindUserSearchResults(List<UserSearchItemDto> users) {
        currentUsers.clear();
        if (users != null) {
            for (UserSearchItemDto dto : users) {
                String username = !TextUtils.isEmpty(dto.getUsername()) ? dto.getUsername() : "user";
                int paletteIdx = Math.abs(username.hashCode()) % AVATAR_PALETTE.length;
                currentUsers.add(new UserItem(
                        username,
                        dto.getAvatarUrl(),
                        (int) dto.getSetCount(),
                        (int) dto.getClassCount(),
                        AVATAR_PALETTE[paletteIdx]
                ));
            }
        }

        boolean hasUsers = !currentUsers.isEmpty();
        binding.llAllUsersHeader.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
        binding.rvAllUsers.setVisibility(hasUsers ? View.VISIBLE : View.GONE);
        allUsersAdapter.submitList(currentUsers.subList(0, Math.min(3, currentUsers.size())));
        singleUsersAdapter.submitList(currentUsers);

        boolean hasAny = !currentSets.isEmpty() || hasUsers;
        binding.tvAllResultsEmpty.setVisibility(hasAny ? View.GONE : View.VISIBLE);
        if (activeTabIndex == 5) {
            renderActiveTab();
        }
    }

    private void openPublicUserProfile(String username) {
        if (TextUtils.isEmpty(username)) return;
        profileViewModel.getPublicProfile(username).observe(getViewLifecycleOwner(), res -> {
            if (res != null && res.getStatus() == com.elearning.smartflash.util.Resource.Status.SUCCESS
                    && res.getData() != null) {
                PublicProfileDialogHelper.show(requireContext(), res.getData());
            } else if (res != null && res.getStatus() == com.elearning.smartflash.util.Resource.Status.ERROR) {
                Toast.makeText(requireContext(), res.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderActiveTab() {
        if (binding == null) return;
        if (activeTabIndex == 0) {
            binding.scrollSearchAllResults.setVisibility(View.VISIBLE);
            binding.llSearchSingleTabContainer.setVisibility(View.GONE);
            return;
        }

        binding.scrollSearchAllResults.setVisibility(View.GONE);
        binding.llSearchSingleTabContainer.setVisibility(View.VISIBLE);
        binding.btnSearchSetsFilter.setVisibility(activeTabIndex == 1 ? View.VISIBLE : View.GONE);
        binding.tvSearchTabEmpty.setVisibility(View.GONE);

        if (activeTabIndex == 1) {
            applySetsFilter();
        } else if (activeTabIndex == 4) {
            showSingleTabListOrEmpty(singleQuestionsAdapter, currentQuestions,
                    "Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
        } else if (activeTabIndex == 5) {
            showSingleTabListOrEmpty(singleUsersAdapter, currentUsers,
                    "Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
        } else if (activeTabIndex == 6) {
            showSingleTabListOrEmpty(singleClassesAdapter, currentClasses,
                    "Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
        } else {
            binding.rvSearchSingleTab.setAdapter(null);
            binding.rvSearchSingleTab.setVisibility(View.GONE);
            binding.tvSearchTabEmpty.setText("Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
            binding.tvSearchTabEmpty.setVisibility(View.VISIBLE);
        }
    }

    private <T> void showSingleTabListOrEmpty(
            androidx.recyclerview.widget.RecyclerView.Adapter<?> adapter,
            List<T> data,
            String emptyMessage
    ) {
        if (data == null || data.isEmpty()) {
            binding.rvSearchSingleTab.setVisibility(View.GONE);
            binding.tvSearchTabEmpty.setText(emptyMessage);
            binding.tvSearchTabEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.tvSearchTabEmpty.setVisibility(View.GONE);
            binding.rvSearchSingleTab.setVisibility(View.VISIBLE);
            binding.rvSearchSingleTab.setAdapter(adapter);
        }
    }

    private void applySetsFilter() {
        List<SetItem> filtered = new ArrayList<>();
        for (SetItem item : currentSets) {
            if (filterTermsMode == 1 && item.termCount >= 20) continue;
            if (filterTermsMode == 2 && (item.termCount < 20 || item.termCount > 49)) continue;
            if (filterTermsMode == 3 && item.termCount < 50) continue;

            if (filterCreatorMode == 1 && !"plus".equals(item.creatorType)) continue;
            if (filterCreatorMode == 2 && !"teacher".equals(item.creatorType)) continue;
            if (filterCreatorMode == 3 && !"verified".equals(item.creatorType)) continue;

            if (filterContentMode == 1 && !item.hasImage) continue;
            if (filterContentMode == 2 && !item.hasDiagram) continue;

            filtered.add(item);
        }

        int activeFilters = (filterTermsMode > 0 ? 1 : 0)
                + (filterCreatorMode > 0 ? 1 : 0)
                + (filterContentMode > 0 ? 1 : 0);
        binding.btnSearchSetsFilter.setText(activeFilters > 0 ? "Bộ lọc (" + activeFilters + ")" : "Bộ lọc");

        binding.rvSearchSingleTab.setAdapter(singleSetsAdapter);
        singleSetsAdapter.submitList(filtered);
        binding.rvSearchSingleTab.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvSearchTabEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        if (filtered.isEmpty()) {
            binding.tvSearchTabEmpty.setText("Không có kết quả cho tìm kiếm của bạn,\nvui lòng thử lại.");
        }
    }

    private void showSetsFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search_filter, null);
        dialog.setContentView(sheet);
        dialog.setOnShowListener(d -> {
            View bs = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bs != null) bs.setBackgroundColor(Color.TRANSPARENT);
        });

        RadioGroup rgTerms = sheet.findViewById(R.id.rg_filter_terms);
        RadioGroup rgCreator = sheet.findViewById(R.id.rg_filter_creator);
        RadioGroup rgContent = sheet.findViewById(R.id.rg_filter_content);

        int[] termIds = {R.id.rb_terms_all, R.id.rb_terms_lt20, R.id.rb_terms_20_49, R.id.rb_terms_50plus};
        int[] creatorIds = {R.id.rb_creator_all, R.id.rb_creator_plus, R.id.rb_creator_teachers, R.id.rb_creator_verified};
        int[] contentIds = {R.id.rb_content_all, R.id.rb_content_images, R.id.rb_content_diagrams};

        rgTerms.check(termIds[filterTermsMode]);
        rgCreator.check(creatorIds[filterCreatorMode]);
        rgContent.check(contentIds[filterContentMode]);

        rgTerms.setOnCheckedChangeListener((g, id) -> {
            for (int i = 0; i < termIds.length; i++) {
                if (termIds[i] == id) filterTermsMode = i;
            }
            applySetsFilter();
        });
        rgCreator.setOnCheckedChangeListener((g, id) -> {
            for (int i = 0; i < creatorIds.length; i++) {
                if (creatorIds[i] == id) filterCreatorMode = i;
            }
            applySetsFilter();
        });
        rgContent.setOnCheckedChangeListener((g, id) -> {
            for (int i = 0; i < contentIds.length; i++) {
                if (contentIds[i] == id) filterContentMode = i;
            }
            applySetsFilter();
        });

        sheet.findViewById(R.id.btn_filter_reset).setOnClickListener(v -> {
            filterTermsMode = 0;
            filterCreatorMode = 0;
            filterContentMode = 0;
            rgTerms.check(R.id.rb_terms_all);
            rgCreator.check(R.id.rb_creator_all);
            rgContent.check(R.id.rb_content_all);
            applySetsFilter();
        });
        sheet.findViewById(R.id.btn_filter_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void lookupPublicProfile(String username) {
        binding.llSearchResultsContainer.setVisibility(View.GONE);
        profileViewModel.getPublicProfile(username).observe(getViewLifecycleOwner(), res -> {
            if (res == null) return;
            switch (res.getStatus()) {
                case LOADING:
                    binding.pbSearchLoading.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.pbSearchLoading.setVisibility(View.GONE);
                    if (res.getData() != null) {
                        lastLoadedProfile = res.getData();
                        bindPublicProfile(res.getData());
                    }
                    break;
                case ERROR:
                    binding.pbSearchLoading.setVisibility(View.GONE);
                    binding.tvSearchFeedback.setText(res.getMessage());
                    binding.tvSearchFeedback.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void bindPublicProfile(PublicProfileDto profile) {
        binding.cardSearchProfileResult.setVisibility(View.VISIBLE);
        String displayName = !TextUtils.isEmpty(profile.getFullName())
                ? profile.getFullName()
                : profile.getUsername();

        binding.tvResultFullName.setText(displayName);
        binding.tvResultMeta.setText("@" + profile.getUsername()
                + " • " + ProfileUtils.getRoleLabel(profile.getRole())
                + " • " + ProfileUtils.getVisibilityLabel(profile.getProfileVisibility()));

        PublicProfileDialogHelper.bindAvatar(
                requireContext(),
                profile.getAvatarUrl(),
                displayName,
                profile.getUsername(),
                binding.ivResultAvatar,
                binding.tvResultInitials
        );

        if (!TextUtils.isEmpty(profile.getBio())) {
            binding.tvResultBio.setText(profile.getBio());
            binding.tvResultBio.setVisibility(View.VISIBLE);
        } else {
            binding.tvResultBio.setVisibility(View.GONE);
        }

        ProfilePrivacyConfigDto tabs = profile.getVisibleTabs();
        List<String> openTabs = new ArrayList<>();
        if (tabs == null || tabs.isShowSets()) openTabs.add("Bộ thẻ");
        if (tabs == null || tabs.isShowFolders()) openTabs.add("Thư mục");
        if (tabs == null || tabs.isShowClasses()) openTabs.add("Lớp học");
        if (tabs == null || tabs.isShowAchievements()) openTabs.add("Thành tựu");

        binding.tvResultTabsSummary.setText(openTabs.isEmpty()
                ? "Người dùng này đã ẩn tất cả các tab."
                : "Các tab được phép xem: " + TextUtils.join(", ", openTabs));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (binding == null) return;
        if (!hidden) {
            if (binding.llSearchResultsContainer.getVisibility() != View.VISIBLE) {
                focusSearchInput();
            }
        } else {
            hideKeyboard();
        }
    }

    private void adjustForKeyboard(View root) {
        if (binding == null) return;
        Rect visibleFrame = new Rect();
        root.getWindowVisibleDisplayFrame(visibleFrame);
        int[] loc = new int[2];
        root.getLocationOnScreen(loc);
        int rootBottom = loc[1] + root.getHeight();
        int overlap = Math.max(0, rootBottom - visibleFrame.bottom);
        int targetPadding = baseBottomPadding + overlap;
        if (targetPadding != lastAppliedBottomPadding) {
            lastAppliedBottomPadding = targetPadding;
            root.setPadding(root.getPaddingLeft(), root.getPaddingTop(), root.getPaddingRight(), targetPadding);
        }
    }

    private void focusSearchInput() {
        if (binding == null) return;
        binding.etSearchQuery.postDelayed(() -> {
            if (binding == null || !isAdded()) return;
            binding.etSearchQuery.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 160L);
    }

    private void hideKeyboard() {
        if (binding == null || !isAdded()) return;
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.etSearchQuery.getWindowToken(), 0);
        }
    }

    private void openSetDetail(SetItem item) {
        if (item == null || item.id <= 0) {
            Toast.makeText(requireContext(), "Học phần mẫu chưa có ID trên máy chủ", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(requireContext(), SetDetailActivity.class);
        intent.putExtra(SetDetailActivity.EXTRA_SET_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (binding != null && keyboardLayoutListener != null) {
            binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
        keyboardLayoutListener = null;
        lastAppliedBottomPadding = -1;
        super.onDestroyView();
        binding = null;
    }
}
