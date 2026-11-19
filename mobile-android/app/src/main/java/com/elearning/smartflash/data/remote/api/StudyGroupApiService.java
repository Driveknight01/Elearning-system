package com.elearning.smartflash.data.remote.api;

import com.elearning.smartflash.data.remote.dto.ApiResponse;
import com.elearning.smartflash.data.remote.dto.StudyGroupSummaryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit2 service for study groups / classes.
 */
public interface StudyGroupApiService {

    @GET("study-groups/my")
    Call<ApiResponse<List<StudyGroupSummaryDto>>> getMyGroups();
}
