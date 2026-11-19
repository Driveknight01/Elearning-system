package com.elearning.smartflash.ui.set;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.elearning.smartflash.data.remote.dto.CreateFlashcardSetRequestDto;
import com.elearning.smartflash.data.remote.dto.FlashcardItemDto;
import com.elearning.smartflash.data.remote.dto.FlashcardItemRequestDto;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;
import com.elearning.smartflash.data.repository.FlashcardSetRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for {@link CreateEditSetActivity} managing set metadata, editable cards, and API persistence.
 */
public class CreateEditSetViewModel extends AndroidViewModel {

    private final FlashcardSetRepository repository;

    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<FlashcardSetDetailDto> savedSet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    private Long editingSetId = null;
    private String title = "";
    private String description = "";
    private boolean descriptionVisible = false;
    private String termLang = null;
    private String defLang = null;
    private String visibility = "PUBLIC";
    private String editableBy = "JUST_ME";
    private boolean autoSuggest = true;
    private final List<EditableCardItem> cards = new ArrayList<>();

    public CreateEditSetViewModel(@NonNull Application application) {
        super(application);
        this.repository = new FlashcardSetRepository(application);
        // Default 2 empty cards matching Quizlet Create Set UI
        cards.add(new EditableCardItem());
        cards.add(new EditableCardItem());
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<FlashcardSetDetailDto> getSavedSet() {
        return savedSet;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public Long getEditingSetId() {
        return editingSetId;
    }

    public boolean isEditMode() {
        return editingSetId != null && editingSetId > 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public boolean isDescriptionVisible() {
        return descriptionVisible;
    }

    public void setDescriptionVisible(boolean descriptionVisible) {
        this.descriptionVisible = descriptionVisible;
    }

    public String getTermLang() {
        return termLang;
    }

    public void setTermLang(String termLang) {
        this.termLang = termLang;
    }

    public String getDefLang() {
        return defLang;
    }

    public void setDefLang(String defLang) {
        this.defLang = defLang;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility != null ? visibility : "PUBLIC";
    }

    public String getEditableBy() {
        return editableBy;
    }

    public void setEditableBy(String editableBy) {
        this.editableBy = editableBy != null ? editableBy : "JUST_ME";
    }

    public boolean isAutoSuggest() {
        return autoSuggest;
    }

    public void setAutoSuggest(boolean autoSuggest) {
        this.autoSuggest = autoSuggest;
    }

    public List<EditableCardItem> getCards() {
        return cards;
    }

    public int addEmptyCard() {
        cards.add(new EditableCardItem());
        return cards.size() - 1;
    }

    public int insertEmptyCardAt(int index) {
        int clamped = Math.max(0, Math.min(index, cards.size()));
        cards.add(clamped, new EditableCardItem());
        return clamped;
    }

    public void loadSetForEdit(long setId, Runnable onLoaded) {
        this.editingSetId = setId;
        loading.setValue(true);
        repository.getSetDetail(setId, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(FlashcardSetDetailDto data) {
                loading.postValue(false);
                if (data != null) {
                    title = data.getTitle() != null ? data.getTitle() : "";
                    description = data.getDescription() != null ? data.getDescription() : "";
                    descriptionVisible = !description.trim().isEmpty();
                    visibility = data.getVisibility() != null ? data.getVisibility() : "PUBLIC";
                    termLang = data.getTermLang();
                    defLang = data.getDefLang();
                    cards.clear();
                    if (data.getCards() != null && !data.getCards().isEmpty()) {
                        for (FlashcardItemDto c : data.getCards()) {
                            cards.add(new EditableCardItem(
                                    c.getId(),
                                    c.getTerm(),
                                    c.getDefinition(),
                                    c.getImageUrl()
                            ));
                        }
                    } else {
                        cards.add(new EditableCardItem());
                        cards.add(new EditableCardItem());
                    }
                }
                if (onLoaded != null) {
                    onLoaded.run();
                }
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void saveSet() {
        String trimmedTitle = title != null ? title.trim() : "";
        if (trimmedTitle.isEmpty()) {
            errorMessage.setValue("Please enter a title for your set");
            return;
        }

        List<FlashcardItemRequestDto> validCards = new ArrayList<>();
        int index = 0;
        for (EditableCardItem item : cards) {
            String term = item.getTerm() != null ? item.getTerm().trim() : "";
            String def = item.getDefinition() != null ? item.getDefinition().trim() : "";
            if (term.isEmpty() && def.isEmpty()) {
                continue;
            }
            if (term.isEmpty() || def.isEmpty()) {
                errorMessage.setValue("Please fill in both term and definition for every card");
                return;
            }
            validCards.add(new FlashcardItemRequestDto(
                    item.getId(),
                    term,
                    def,
                    item.getImageUrl(),
                    index++
            ));
        }

        if (validCards.isEmpty()) {
            errorMessage.setValue("Please add at least one card with term and definition");
            return;
        }

        CreateFlashcardSetRequestDto request = new CreateFlashcardSetRequestDto();
        request.setTitle(trimmedTitle);
        String trimmedDesc = description != null ? description.trim() : "";
        request.setDescription(trimmedDesc.isEmpty() ? null : trimmedDesc);
        request.setVisibility(visibility);
        request.setTermLang(termLang != null && !termLang.isEmpty() ? termLang : null);
        request.setDefLang(defLang != null && !defLang.isEmpty() ? defLang : null);
        request.setSourceType("MANUAL");
        request.setCards(validCards);

        saving.setValue(true);
        FlashcardSetRepository.ResultCallback<FlashcardSetDetailDto> callback =
                new FlashcardSetRepository.ResultCallback<>() {
                    @Override
                    public void onSuccess(FlashcardSetDetailDto data) {
                        saving.postValue(false);
                        savedSet.postValue(data);
                    }

                    @Override
                    public void onError(String message) {
                        saving.postValue(false);
                        errorMessage.postValue(message);
                    }
                };

        if (isEditMode()) {
            repository.updateSet(editingSetId, request, callback);
        } else {
            repository.createSet(request, callback);
        }
    }

    public void deleteSetIfEditing() {
        if (!isEditMode()) {
            deleted.setValue(true);
            return;
        }
        saving.setValue(true);
        repository.deleteSet(editingSetId, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(Void data) {
                saving.postValue(false);
                deleted.postValue(true);
            }

            @Override
            public void onError(String message) {
                saving.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
}
