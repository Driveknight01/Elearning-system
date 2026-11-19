package com.elearning.smartflash.ui.set;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;
import com.elearning.smartflash.data.remote.dto.SetInteractionDto;
import com.elearning.smartflash.data.remote.dto.StarredCardsDto;
import com.elearning.smartflash.data.repository.FlashcardSetRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * ViewModel for {@link SetDetailActivity} managing set metadata, interactions, starred cards, clone, and delete.
 */
public class SetDetailViewModel extends AndroidViewModel {

    private final FlashcardSetRepository repository;
    private final TokenManager tokenManager;

    private final MutableLiveData<FlashcardSetDetailDto> setDetail = new MutableLiveData<>();
    private final MutableLiveData<Set<Long>> starredCardIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> userRating = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();
    private final MutableLiveData<Long> clonedSetId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deletedSuccess = new MutableLiveData<>(false);

    private long currentSetId = -1L;
    private String currentShareToken;

    public SetDetailViewModel(@NonNull Application application) {
        super(application);
        this.repository = new FlashcardSetRepository(application);
        this.tokenManager = TokenManager.getInstance(application);
    }

    public LiveData<FlashcardSetDetailDto> getSetDetail() {
        return setDetail;
    }

    public LiveData<Set<Long>> getStarredCardIds() {
        return starredCardIds;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public LiveData<Integer> getUserRating() {
        return userRating;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public LiveData<Long> getClonedSetId() {
        return clonedSetId;
    }

    public LiveData<Boolean> getDeletedSuccess() {
        return deletedSuccess;
    }

    public void loadSet(long setId, String shareToken) {
        this.currentSetId = setId;
        this.currentShareToken = shareToken;
        loading.setValue(true);

        FlashcardSetRepository.ResultCallback<FlashcardSetDetailDto> callback = new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(FlashcardSetDetailDto data) {
                loading.setValue(false);
                if (data == null) {
                    errorMessage.setValue("Không tìm thấy dữ liệu học phần");
                    return;
                }
                currentSetId = data.getId() != null ? data.getId() : setId;
                if (currentShareToken == null && data.getShareToken() != null) {
                    currentShareToken = data.getShareToken();
                }
                setDetail.setValue(data);
                if (tokenManager.isLoggedIn() && currentSetId > 0) {
                    loadUserInteractions(currentSetId);
                }
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                errorMessage.setValue(message);
            }
        };

        if (setId > 0) {
            repository.getSetDetail(setId, shareToken, callback);
        } else if (shareToken != null && !shareToken.trim().isEmpty()) {
            repository.getSetByShareToken(shareToken.trim(), callback);
        } else {
            loading.setValue(false);
            errorMessage.setValue("Thiếu mã học phần để hiển thị");
        }
    }

    private void loadUserInteractions(long setId) {
        repository.getInteraction(setId, currentShareToken, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(SetInteractionDto data) {
                if (data != null) {
                    saved.setValue(data.isSaved());
                    userRating.setValue(data.getRating() != null ? data.getRating() : 0);
                }
            }

            @Override
            public void onError(String message) {
                // Non-fatal: keep default saved/rating state
            }
        });

        repository.getStarredCards(setId, currentShareToken, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(StarredCardsDto data) {
                Set<Long> next = new HashSet<>();
                if (data != null && data.getStarredCardIds() != null) {
                    next.addAll(data.getStarredCardIds());
                }
                starredCardIds.setValue(next);
            }

            @Override
            public void onError(String message) {
                // Non-fatal
            }
        });
    }

    public void toggleSave() {
        if (!tokenManager.isLoggedIn()) {
            actionMessage.setValue("Vui lòng đăng nhập để lưu học phần");
            return;
        }
        if (currentSetId <= 0) return;

        boolean previous = Boolean.TRUE.equals(saved.getValue());
        boolean next = !previous;
        saved.setValue(next);

        repository.saveSet(currentSetId, currentShareToken, next, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(SetInteractionDto data) {
                actionMessage.setValue(next ? "Đã lưu học phần vào thư viện" : "Đã bỏ lưu học phần");
            }

            @Override
            public void onError(String message) {
                saved.setValue(previous);
                actionMessage.setValue(message);
            }
        });
    }

    public void toggleStarCard(long cardId) {
        if (!tokenManager.isLoggedIn()) {
            actionMessage.setValue("Vui lòng đăng nhập để gắn sao thẻ khó");
            return;
        }
        if (currentSetId <= 0 || cardId <= 0) return;

        Set<Long> current = starredCardIds.getValue();
        Set<Long> updated = current != null ? new HashSet<>(current) : new HashSet<>();
        boolean willStar = !updated.contains(cardId);
        if (willStar) {
            updated.add(cardId);
        } else {
            updated.remove(cardId);
        }
        starredCardIds.setValue(updated);

        repository.toggleStarCard(currentSetId, cardId, currentShareToken, willStar, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(Void data) {
                // Synced
            }

            @Override
            public void onError(String message) {
                Set<Long> rollback = starredCardIds.getValue();
                Set<Long> restored = rollback != null ? new HashSet<>(rollback) : new HashSet<>();
                if (willStar) {
                    restored.remove(cardId);
                } else {
                    restored.add(cardId);
                }
                starredCardIds.setValue(restored);
                actionMessage.setValue(message);
            }
        });
    }

    public void rateSet(int stars, String comment) {
        if (!tokenManager.isLoggedIn()) {
            actionMessage.setValue("Vui lòng đăng nhập để đánh giá học phần");
            return;
        }
        if (currentSetId <= 0) return;

        repository.rateSet(currentSetId, currentShareToken, stars, comment, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(SetInteractionDto data) {
                userRating.setValue(stars);
                actionMessage.setValue("Cảm ơn bạn đã đánh giá " + stars + " sao");
                loadSet(currentSetId, currentShareToken);
            }

            @Override
            public void onError(String message) {
                actionMessage.setValue(message);
            }
        });
    }

    public void cloneSet() {
        if (!tokenManager.isLoggedIn()) {
            actionMessage.setValue("Vui lòng đăng nhập để nhân bản học phần");
            return;
        }
        if (currentSetId <= 0) return;

        loading.setValue(true);
        repository.cloneSet(currentSetId, currentShareToken, null, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(FlashcardSetDetailDto data) {
                loading.setValue(false);
                if (data != null && data.getId() != null) {
                    actionMessage.setValue("Đã nhân bản học phần về thư viện của bạn");
                    clonedSetId.setValue(data.getId());
                }
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                actionMessage.setValue(message);
            }
        });
    }

    public void deleteSet() {
        if (currentSetId <= 0) return;
        loading.setValue(true);
        repository.deleteSet(currentSetId, new FlashcardSetRepository.ResultCallback<>() {
            @Override
            public void onSuccess(Void data) {
                loading.setValue(false);
                deletedSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                actionMessage.setValue(message);
            }
        });
    }

    public void clearActionMessage() {
        actionMessage.setValue(null);
    }
}
