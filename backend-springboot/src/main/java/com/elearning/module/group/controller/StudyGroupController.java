package com.elearning.module.group.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.module.group.dto.CreateStudyGroupRequest;
import com.elearning.module.group.dto.JoinStudyGroupRequest;
import com.elearning.module.group.dto.StudyGroupDetailResponse;
import com.elearning.module.group.dto.StudyGroupSummaryResponse;
import com.elearning.module.group.service.StudyGroupService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for peer study groups: create, join via invite code, manage members, and share sets.
 */
@RestController
@RequestMapping("/api/v1/study-groups")
@RequiredArgsConstructor
@Tag(name = "Study Groups", description = "Tạo nhóm học, tham gia bằng mã mời và chia sẻ bộ thẻ trong nhóm")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    @PostMapping
    @Operation(summary = "Tạo nhóm học mới (tự sinh invite_code)")
    public ResponseEntity<ApiResponse<StudyGroupDetailResponse>> createGroup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateStudyGroupRequest request
    ) {
        StudyGroupDetailResponse created = studyGroupService.createGroup(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo nhóm học thành công", created));
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách nhóm học mà người dùng đang tham gia")
    public ResponseEntity<ApiResponse<List<StudyGroupSummaryResponse>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.getMyGroups(currentUser.getId())));
    }

    @PostMapping("/join")
    @Operation(summary = "Tham gia nhóm học bằng mã mời (invite_code)")
    public ResponseEntity<ApiResponse<StudyGroupDetailResponse>> joinGroup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody JoinStudyGroupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tham gia nhóm học thành công",
                studyGroupService.joinByInviteCode(currentUser.getId(), request.getInviteCode())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết nhóm học (thành viên và bộ thẻ được chia sẻ)")
    public ResponseEntity<ApiResponse<StudyGroupDetailResponse>> getGroupDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.getGroupDetail(id, currentUser)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa nhóm học (chỉ chủ nhóm hoặc Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        studyGroupService.deleteGroup(id, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Đã xóa nhóm học"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Rời nhóm học hoặc xóa thành viên khỏi nhóm")
    public ResponseEntity<ApiResponse<Void>> removeOrLeaveMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        studyGroupService.removeOrLeaveMember(id, userId, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Đã cập nhật thành viên nhóm"));
    }

    @PostMapping("/{id}/sets/{setId}")
    @Operation(summary = "Chia sẻ bộ thẻ vào nhóm học")
    public ResponseEntity<ApiResponse<StudyGroupDetailResponse>> shareSetToGroup(
            @PathVariable Long id,
            @PathVariable Long setId,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã chia sẻ bộ thẻ vào nhóm",
                studyGroupService.shareSetToGroup(id, setId, token, currentUser)
        ));
    }

    @DeleteMapping("/{id}/sets/{setId}")
    @Operation(summary = "Gỡ bộ thẻ khỏi nhóm học")
    public ResponseEntity<ApiResponse<StudyGroupDetailResponse>> removeSetFromGroup(
            @PathVariable Long id,
            @PathVariable Long setId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã gỡ bộ thẻ khỏi nhóm",
                studyGroupService.removeSetFromGroup(id, setId, currentUser)
        ));
    }
}
