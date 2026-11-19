package com.elearning.smartflash.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Summary response matching Spring Boot {@code StudyGroupSummaryResponse}.
 */
public class StudyGroupSummaryDto {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("inviteCode")
    private String inviteCode;

    @SerializedName("memberCount")
    private long memberCount;

    @SerializedName("setCount")
    private int setCount;

    @SerializedName("myRole")
    private String myRole;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public int getSetCount() {
        return setCount;
    }

    public String getMyRole() {
        return myRole;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
