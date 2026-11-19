package com.elearning.smartflash.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.elearning.smartflash.data.remote.api.FlashcardSetApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.CloneSetRequestDto;
import com.elearning.smartflash.data.remote.dto.CreateFlashcardSetRequestDto;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;
import com.elearning.smartflash.data.remote.dto.OcrExtractResponseDto;
import com.elearning.smartflash.data.remote.dto.RateSetRequestDto;
import com.elearning.smartflash.data.remote.dto.SetInteractionDto;
import com.elearning.smartflash.data.remote.dto.StarredCardsDto;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository handling CRUD operations for flashcard sets via Retrofit2.
 */
public class FlashcardSetRepository {

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final FlashcardSetApiService apiService;

    public FlashcardSetRepository(Context context) {
        this.apiService = RetrofitClient.getInstance(context).getFlashcardSetApi();
    }

    public void createSet(CreateFlashcardSetRequestDto request, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.createSet(request).enqueue(wrapCallback(callback));
    }

    public void getSetDetail(long setId, ResultCallback<FlashcardSetDetailDto> callback) {
        getSetDetail(setId, null, callback);
    }

    public void getSetDetail(long setId, String shareToken, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.getSetDetail(setId, shareToken).enqueue(wrapCallback(callback));
    }

    public void getSetByShareToken(String shareToken, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.getSetByShareToken(shareToken).enqueue(wrapCallback(callback));
    }

    public void cloneSet(long setId, String shareToken, CloneSetRequestDto request, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.cloneSet(setId, shareToken, request != null ? request : new CloneSetRequestDto(null, null))
                .enqueue(wrapCallback(callback));
    }

    public void saveSet(long setId, String shareToken, boolean save, ResultCallback<SetInteractionDto> callback) {
        Call<ApiResponse<SetInteractionDto>> call = save
                ? apiService.saveSet(setId, shareToken)
                : apiService.unsaveSet(setId, shareToken);
        call.enqueue(wrapCallback(callback));
    }

    public void getInteraction(long setId, String shareToken, ResultCallback<SetInteractionDto> callback) {
        apiService.getInteraction(setId, shareToken).enqueue(wrapCallback(callback));
    }

    public void rateSet(long setId, String shareToken, int rating, String comment, ResultCallback<SetInteractionDto> callback) {
        apiService.rateSet(setId, shareToken, new RateSetRequestDto(rating, comment))
                .enqueue(wrapCallback(callback));
    }

    public void toggleStarCard(long setId, long cardId, String shareToken, boolean star, ResultCallback<Void> callback) {
        Call<ApiResponse<Void>> call = star
                ? apiService.starCard(setId, cardId, shareToken)
                : apiService.unstarCard(setId, cardId);
        call.enqueue(wrapVoidCallback(callback));
    }

    public void getStarredCards(long setId, String shareToken, ResultCallback<StarredCardsDto> callback) {
        apiService.getStarredCards(setId, shareToken).enqueue(wrapCallback(callback));
    }

    public void updateSet(long setId, CreateFlashcardSetRequestDto request, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.updateSet(setId, request).enqueue(wrapCallback(callback));
    }

    public void deleteSet(long setId, ResultCallback<Void> callback) {
        apiService.deleteSet(setId).enqueue(wrapVoidCallback(callback));
    }

    private Callback<ApiResponse<Void>> wrapVoidCallback(ResultCallback<Void> callback) {
        return new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        };
    }

    public void extractOcrCards(File imageFile, String sourceLang, String targetLang,
                                ResultCallback<OcrExtractResponseDto> callback) {
        RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", imageFile.getName(), fileBody);
        RequestBody srcPart = sourceLang != null && !sourceLang.isEmpty()
                ? RequestBody.create(sourceLang, MediaType.parse("text/plain"))
                : null;
        RequestBody dstPart = targetLang != null && !targetLang.isEmpty()
                ? RequestBody.create(targetLang, MediaType.parse("text/plain"))
                : null;
        apiService.extractOcrCards(part, srcPart, dstPart).enqueue(wrapCallback(callback));
    }

    private <T> Callback<ApiResponse<T>> wrapCallback(ResultCallback<T> callback) {
        return new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<T>> call,
                                   @NonNull Response<ApiResponse<T>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<T>> call, @NonNull Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Lỗi kết nối máy chủ");
            }
        };
    }

    private String parseErrorMessage(Response<?> response) {
        if (response.body() instanceof ApiResponse<?> apiResp && apiResp.getMessage() != null) {
            return apiResp.getMessage();
        }
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JSONObject json = new JSONObject(raw);
                if (json.has("message")) {
                    return json.getString("message");
                }
            }
        } catch (Exception ignored) {
        }
        return "Không thể xử lý yêu cầu (mã " + response.code() + ")";
    }
}
