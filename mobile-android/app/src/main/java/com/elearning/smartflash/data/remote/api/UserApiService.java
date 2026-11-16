package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.AvatarUploadResponseDto;
import com.elearning.smartflash.data.remote.dto.ChangePasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.MyProfileDto;
import com.elearning.smartflash.data.remote.dto.PublicProfileDto;
import com.elearning.smartflash.data.remote.dto.UpdatePrivacyRequestDto;
import com.elearning.smartflash.data.remote.dto.UpdateProfileRequestDto;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit2 service definitions for user profile, avatar upload, password change,
 * granular privacy settings, and public profile viewing.
 */
public interface UserApiService {

    @GET("users/me")
    Call<ApiResponse<MyProfileDto>> getMyProfile();

    @PUT("users/me")
    Call<ApiResponse<MyProfileDto>> updateMyProfile(@Body UpdateProfileRequestDto request);

    @Multipart
    @POST("users/me/avatar")
    Call<ApiResponse<AvatarUploadResponseDto>> uploadAvatar(@Part MultipartBody.Part file);

    @PUT("users/me/password")
    Call<ApiResponse<Void>> changePassword(@Body ChangePasswordRequestDto request);

    @PUT("users/me/privacy")
    Call<ApiResponse<MyProfileDto>> updatePrivacy(@Body UpdatePrivacyRequestDto request);

    @GET("users/profile/{username}")
    Call<ApiResponse<PublicProfileDto>> getPublicProfile(@Path("username") String username);
}
