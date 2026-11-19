package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.AuthResponseDto;
import com.elearning.smartflash.data.remote.dto.ForgotPasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.GoogleLoginRequestDto;
import com.elearning.smartflash.data.remote.dto.LoginRequestDto;
import com.elearning.smartflash.data.remote.dto.LogoutRequestDto;
import com.elearning.smartflash.data.remote.dto.RefreshTokenRequestDto;
import com.elearning.smartflash.data.remote.dto.RegisterRequestDto;
import com.elearning.smartflash.data.remote.dto.ResetPasswordRequestDto;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit2 service definitions for authentication and session endpoints.
 */
public interface AuthApiService {

    @GET("auth/check-email")
    Call<ApiResponse<Boolean>> checkEmail(@Query("email") String email);

    @POST("auth/register")
    Call<ApiResponse<AuthResponseDto>> register(@Body RegisterRequestDto request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponseDto>> login(@Body LoginRequestDto request);

    @POST("auth/google")
    Call<ApiResponse<AuthResponseDto>> googleLogin(@Body GoogleLoginRequestDto request);

    @POST("auth/forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body ForgotPasswordRequestDto request);

    @POST("auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body ResetPasswordRequestDto request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponseDto>> refresh(@Body RefreshTokenRequestDto request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout(@Body LogoutRequestDto request);

    @GET("users/me")
    Call<ApiResponse<UserSummaryDto>> getMe();
}
