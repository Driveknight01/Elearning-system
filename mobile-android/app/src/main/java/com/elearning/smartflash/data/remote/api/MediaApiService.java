package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.MediaUploadResponseDto;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit2 service for uploading flashcard illustration images.
 */
public interface MediaApiService {

    @Multipart
    @POST("media/upload")
    Call<ApiResponse<MediaUploadResponseDto>> uploadCardImage(@Part MultipartBody.Part file);
}
