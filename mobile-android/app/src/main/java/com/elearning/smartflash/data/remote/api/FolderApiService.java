package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.FolderDetailDto;
import com.elearning.smartflash.data.remote.dto.FolderRequestDto;
import com.elearning.smartflash.data.remote.dto.FolderSummaryDto;
import com.elearning.smartflash.data.remote.dto.PageResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit2 service for folder creation, detail lookup, and set membership management.
 */
public interface FolderApiService {

    @GET("folders/my")
    Call<ApiResponse<PageResponseDto<FolderSummaryDto>>> getMyFolders(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("folders/{id}")
    Call<ApiResponse<FolderDetailDto>> getFolderById(@Path("id") long id);

    @POST("folders")
    Call<ApiResponse<FolderSummaryDto>> createFolder(@Body FolderRequestDto request);

    @PUT("folders/{id}")
    Call<ApiResponse<FolderSummaryDto>> updateFolder(
            @Path("id") long id,
            @Body FolderRequestDto request
    );

    @DELETE("folders/{id}")
    Call<ApiResponse<Void>> deleteFolder(@Path("id") long id);

    @POST("folders/{id}/sets/{setId}")
    Call<ApiResponse<FolderDetailDto>> addSetToFolder(
            @Path("id") long folderId,
            @Path("setId") long setId
    );

    @DELETE("folders/{id}/sets/{setId}")
    Call<ApiResponse<FolderDetailDto>> removeSetFromFolder(
            @Path("id") long folderId,
            @Path("setId") long setId
    );
}
