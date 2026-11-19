package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.CreateFlashcardSetRequestDto;
import com.elearning.smartflash.data.remote.dto.FlashcardSetDetailDto;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;
import com.elearning.smartflash.data.remote.dto.SetSummaryDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit2 service for creating, listing, searching, updating, and deleting flashcard sets.
 */
public interface FlashcardSetApiService {

    @GET("flashcard-sets/my")
    Call<ApiResponse<PageResponseDto<SetSummaryDto>>> getMySets(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("flashcard-sets/search")
    Call<ApiResponse<PageResponseDto<SetSummaryDto>>> searchPublicSets(
            @Query("keyword") String keyword,
            @Query("categoryId") Long categoryId,
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @POST("flashcard-sets")
    Call<ApiResponse<FlashcardSetDetailDto>> createSet(@Body CreateFlashcardSetRequestDto request);

    @GET("flashcard-sets/{id}")
    Call<ApiResponse<FlashcardSetDetailDto>> getSetDetail(@Path("id") long setId);

    @PUT("flashcard-sets/{id}")
    Call<ApiResponse<FlashcardSetDetailDto>> updateSet(
            @Path("id") long setId,
            @Body CreateFlashcardSetRequestDto request
    );

    @DELETE("flashcard-sets/{id}")
    Call<ApiResponse<Void>> deleteSet(@Path("id") long setId);
}
