package com.elearning.smartflash.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.elearning.smartflash.data.remote.api.FlashcardSetApiService;
import com.elearning.smartflash.data.remote.api.RetrofitClient;
import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.CreateFlashcardSetRequestDto;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;

import org.json.JSONObject;

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
        apiService.getSetDetail(setId).enqueue(wrapCallback(callback));
    }

    public void updateSet(long setId, CreateFlashcardSetRequestDto request, ResultCallback<FlashcardSetDetailDto> callback) {
        apiService.updateSet(setId, request).enqueue(wrapCallback(callback));
    }

    public void deleteSet(long setId, ResultCallback<Void> callback) {
        apiService.deleteSet(setId).enqueue(new Callback<>() {
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
        });
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
