package com.elearning.smartflash.ui.library;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.api.FlashcardSetApiService;
import com.elearning.smartflash.data.remote.api.FolderApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.api.StudyGroupApiService;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;
import com.elearning.smartflash.data.remote.dto.StudyGroupSummaryDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.databinding.FragmentLibraryBinding;
import com.elearning.smartflash.ui.folder.CreateFolderActivity;
import com.elearning.smartflash.ui.folder.FolderDetailActivity;
import com.elearning.smartflash.ui.main.MainActivity;
import com.elearning.smartflash.ui.set.SetDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Quizlet-style Library tab with 4 pill tabs:
 * Học phần (Flashcard sets), Lớp học (Classes), Thư mục (Folders), Bài kiểm tra thử (Practice tests).
 */
public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private int activeTabIndex = 0;
    private String activeSetFilter = "Tất cả";
    private String currentSearchQuery = "";

    private LibrarySetAdapter setAdapter;
    private LibraryFolderAdapter folderAdapter;
    private LibraryClassAdapter classAdapter;
    private final List<SetSummaryDto> allMySets = new ArrayList<>();
    private final List<FolderSummaryDto> allMyFolders = new ArrayList<>();
    private final List<StudyGroupSummaryDto> allMyClasses = new ArrayList<>();

    private FlashcardSetApiService flashcardSetApiService;
    private FolderApiService folderApiService;
    private StudyGroupApiService studyGroupApiService;
    private Long currentUserId;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        flashcardSetApiService = RetrofitClient.getInstance(requireContext()).getFlashcardSetApi();
        folderApiService = RetrofitClient.getInstance(requireContext()).getFolderApi();
        studyGroupApiService = RetrofitClient.getInstance(requireContext()).getStudyGroupApi();

        UserSummaryDto user = TokenManager.getInstance(requireContext()).getUser();
        currentUserId = user != null ? user.getId() : null;

        setupLists();
        setupActions();
        selectTab(0);
        loadLibraryData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            loadLibraryData();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            loadLibraryData();
        }
    }

    private void setupLists() {
        setAdapter = new LibrarySetAdapter(currentUserId, set -> {
            if (set.getId() != null) {
                Intent intent = new Intent(requireContext(), SetDetailActivity.class);
                intent.putExtra(SetDetailActivity.EXTRA_SET_ID, set.getId());
                startActivity(intent);
            }
        });
        binding.rvLibrarySets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLibrarySets.setAdapter(setAdapter);

        folderAdapter = new LibraryFolderAdapter(folder -> {
            if (folder.getId() != null) {
                Intent intent = new Intent(requireContext(), FolderDetailActivity.class);
                intent.putExtra(FolderDetailActivity.EXTRA_FOLDER_ID, folder.getId());
                intent.putExtra(FolderDetailActivity.EXTRA_FOLDER_NAME, folder.getName());
                startActivity(intent);
            }
        });
        binding.rvLibraryFolders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLibraryFolders.setAdapter(folderAdapter);

        classAdapter = new LibraryClassAdapter(group -> {
            String name = !TextUtils.isEmpty(group.getName()) ? group.getName() : "Lớp học";
            Toast.makeText(
                    requireContext(),
                    "Lớp \"" + name + "\" (" + group.getSetCount() + " học phần, " + group.getMemberCount() + " thành viên)",
                    Toast.LENGTH_SHORT
            ).show();
        });
        binding.rvLibraryClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLibraryClasses.setAdapter(classAdapter);
    }

    private void setupActions() {
        binding.btnLibraryAdd.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity activity) {
                activity.showCreateBottomSheet();
            }
        });

        binding.pillTabSets.setOnClickListener(v -> selectTab(0));
        binding.pillTabClasses.setOnClickListener(v -> selectTab(1));
        binding.pillTabFolders.setOnClickListener(v -> selectTab(2));
        binding.pillTabTests.setOnClickListener(v -> selectTab(3));

        binding.btnSetsFilter.setOnClickListener(this::showSetFilterPopup);
        binding.btnLibraryCreateFolder.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateFolderActivity.class))
        );

        binding.etLibrarySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                applySetFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadLibraryData() {
        loadMySets();
        loadMyFolders();
        loadMyClasses();
    }

    private void loadMySets() {
        if (binding == null) return;
        if (allMySets.isEmpty()) {
            binding.pbLibrarySets.setVisibility(View.VISIBLE);
            binding.llSetsEmptyWrapper.setVisibility(View.GONE);
        }

        flashcardSetApiService.getMySets(0, 50).enqueue(new Callback<ApiResponse<PageResponseDto<SetSummaryDto>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                    @NonNull Response<ApiResponse<PageResponseDto<SetSummaryDto>>> response
            ) {
                if (binding == null) return;
                binding.pbLibrarySets.setVisibility(View.GONE);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    allMySets.clear();
                    List<SetSummaryDto> content = response.body().getData().getContent();
                    if (content != null) {
                        allMySets.addAll(content);
                    }
                    applySetFilters();
                } else if (allMySets.isEmpty()) {
                    applySetFilters();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PageResponseDto<SetSummaryDto>>> call,
                    @NonNull Throwable t
            ) {
                if (binding == null) return;
                binding.pbLibrarySets.setVisibility(View.GONE);
                if (allMySets.isEmpty()) {
                    applySetFilters();
                }
            }
        });
    }

    private void loadMyFolders() {
        if (binding == null) return;
        if (allMyFolders.isEmpty()) {
            binding.pbLibraryFolders.setVisibility(View.VISIBLE);
            binding.llFoldersEmpty.setVisibility(View.GONE);
        }

        folderApiService.getMyFolders(0, 50).enqueue(new Callback<ApiResponse<PageResponseDto<FolderSummaryDto>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PageResponseDto<FolderSummaryDto>>> call,
                    @NonNull Response<ApiResponse<PageResponseDto<FolderSummaryDto>>> response
            ) {
                if (binding == null) return;
                binding.pbLibraryFolders.setVisibility(View.GONE);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    allMyFolders.clear();
                    List<FolderSummaryDto> content = response.body().getData().getContent();
                    if (content != null) {
                        allMyFolders.addAll(content);
                    }
                    renderFolders();
                } else if (allMyFolders.isEmpty()) {
                    renderFolders();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<PageResponseDto<FolderSummaryDto>>> call,
                    @NonNull Throwable t
            ) {
                if (binding == null) return;
                binding.pbLibraryFolders.setVisibility(View.GONE);
                if (allMyFolders.isEmpty()) {
                    renderFolders();
                }
            }
        });
    }

    private void applySetFilters() {
        if (binding == null) return;
        String queryLower = currentSearchQuery.toLowerCase(Locale.ROOT);
        List<SetSummaryDto> filtered = new ArrayList<>();
        for (SetSummaryDto set : allMySets) {
            if ("Đã tạo".equals(activeSetFilter) && currentUserId != null) {
                if (set.getOwner() == null || !currentUserId.equals(set.getOwner().getId())) {
                    continue;
                }
            } else if ("Đã tải về".equals(activeSetFilter)) {
                // Offline RoomDB filter will be connected in Phase 5
                continue;
            }

            if (!TextUtils.isEmpty(queryLower)) {
                String title = set.getTitle() != null ? set.getTitle().toLowerCase(Locale.ROOT) : "";
                String desc = set.getDescription() != null ? set.getDescription().toLowerCase(Locale.ROOT) : "";
                if (!title.contains(queryLower) && !desc.contains(queryLower)) {
                    continue;
                }
            }
            filtered.add(set);
        }

        if (filtered.isEmpty()) {
            binding.rvLibrarySets.setVisibility(View.GONE);
            binding.llSetsEmptyWrapper.setVisibility(View.VISIBLE);
            binding.tvSetsEmptyMessage.setText(TextUtils.isEmpty(queryLower)
                    ? "Các học phần bạn tạo hoặc đã học sẽ hiển thị tại đây."
                    : "Không tìm thấy học phần nào khớp với \"" + currentSearchQuery + "\".");
        } else {
            binding.llSetsEmptyWrapper.setVisibility(View.GONE);
            binding.rvLibrarySets.setVisibility(View.VISIBLE);
            setAdapter.submitSets(filtered);
        }
    }

    private void loadMyClasses() {
        if (binding == null) return;
        if (allMyClasses.isEmpty()) {
            binding.pbLibraryClasses.setVisibility(View.VISIBLE);
            binding.llClassesEmpty.setVisibility(View.GONE);
        }

        studyGroupApiService.getMyGroups().enqueue(new Callback<ApiResponse<List<StudyGroupSummaryDto>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<List<StudyGroupSummaryDto>>> call,
                    @NonNull Response<ApiResponse<List<StudyGroupSummaryDto>>> response
            ) {
                if (binding == null) return;
                binding.pbLibraryClasses.setVisibility(View.GONE);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    allMyClasses.clear();
                    allMyClasses.addAll(response.body().getData());
                    renderClasses();
                } else if (allMyClasses.isEmpty()) {
                    renderClasses();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<List<StudyGroupSummaryDto>>> call,
                    @NonNull Throwable t
            ) {
                if (binding == null) return;
                binding.pbLibraryClasses.setVisibility(View.GONE);
                if (allMyClasses.isEmpty()) {
                    renderClasses();
                }
            }
        });
    }

    private void renderClasses() {
        if (binding == null) return;
        if (allMyClasses.isEmpty()) {
            binding.rvLibraryClasses.setVisibility(View.GONE);
            binding.llClassesEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.llClassesEmpty.setVisibility(View.GONE);
            binding.rvLibraryClasses.setVisibility(View.VISIBLE);
            classAdapter.submitGroups(allMyClasses);
        }
    }

    private void renderFolders() {
        if (binding == null) return;
        if (allMyFolders.isEmpty()) {
            binding.rvLibraryFolders.setVisibility(View.GONE);
            binding.llFoldersEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.llFoldersEmpty.setVisibility(View.GONE);
            binding.rvLibraryFolders.setVisibility(View.VISIBLE);
            folderAdapter.submitFolders(allMyFolders);
        }
    }

    private void selectTab(int index) {
        activeTabIndex = index;
        TextView[] pills = {
                binding.pillTabSets,
                binding.pillTabClasses,
                binding.pillTabFolders,
                binding.pillTabTests
        };
        View[] sections = {
                binding.llTabSetsContent,
                binding.llTabClassesContent,
                binding.llTabFoldersContent,
                binding.llTabTestsContent
        };

        for (int i = 0; i < pills.length; i++) {
            boolean selected = (i == index);
            pills[i].setBackgroundResource(
                    selected ? R.drawable.bg_library_pill_active : R.drawable.bg_library_pill_inactive
            );
            pills[i].setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? R.color.sf_text_primary : R.color.sf_text_secondary
            ));
            sections[i].setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    private void showSetFilterPopup(View anchor) {
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_library_filter, null);
        PopupWindow popup = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(8f);

        popupView.findViewById(R.id.iv_check_all).setVisibility("Tất cả".equals(activeSetFilter) ? View.VISIBLE : View.GONE);
        popupView.findViewById(R.id.iv_check_created).setVisibility("Đã tạo".equals(activeSetFilter) ? View.VISIBLE : View.GONE);
        popupView.findViewById(R.id.iv_check_studied).setVisibility("Đã học".equals(activeSetFilter) ? View.VISIBLE : View.GONE);
        popupView.findViewById(R.id.iv_check_downloaded).setVisibility("Đã tải về".equals(activeSetFilter) ? View.VISIBLE : View.GONE);

        bindFilterOption(popupView.findViewById(R.id.opt_filter_all), "Tất cả", popup);
        bindFilterOption(popupView.findViewById(R.id.opt_filter_created), "Đã tạo", popup);
        bindFilterOption(popupView.findViewById(R.id.opt_filter_studied), "Đã học", popup);
        bindFilterOption(popupView.findViewById(R.id.opt_filter_downloaded), "Đã tải về", popup);

        popup.showAsDropDown(anchor, 0, 8);
    }

    private void bindFilterOption(View optionView, String label, PopupWindow popup) {
        optionView.setOnClickListener(v -> {
            activeSetFilter = label;
            binding.tvSetsFilterLabel.setText(label);
            popup.dismiss();
            applySetFilters();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
