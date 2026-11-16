package com.elearning.module.flashcard.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.common.dto.PageResponse;
import com.elearning.module.flashcard.dto.FolderDetailResponse;
import com.elearning.module.flashcard.dto.FolderRequest;
import com.elearning.module.flashcard.dto.FolderSummaryResponse;
import com.elearning.module.flashcard.service.FolderService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing folders and their flashcard set associations.
 */
@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Tag(name = "Folders", description = "CRUD thư mục và thêm/xóa bộ thẻ trong thư mục")
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    @Operation(summary = "Tạo thư mục mới")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> createFolder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FolderRequest request
    ) {
        FolderDetailResponse created = folderService.createFolder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thư mục thành công", created));
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách thư mục của người dùng hiện tại")
    public ResponseEntity<ApiResponse<PageResponse<FolderSummaryResponse>>> getMyFolders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.success(folderService.getMyFolders(currentUser.getId(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết thư mục và các bộ thẻ bên trong")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> getFolderById(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(folderService.getFolderById(id, token, currentUser)));
    }

    @GetMapping("/share/{shareToken}")
    @Operation(summary = "Truy cập thư mục qua mã chia sẻ share_token")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> getFolderByShareToken(
            @PathVariable String shareToken,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(folderService.getFolderByShareToken(shareToken, currentUser)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin thư mục")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> updateFolder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FolderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thư mục thành công",
                folderService.updateFolder(id, currentUser, request)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thư mục")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        folderService.deleteFolder(id, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Xóa thư mục thành công"));
    }

    @PostMapping("/{id}/sets/{setId}")
    @Operation(summary = "Thêm bộ thẻ vào thư mục")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> addSetToFolder(
            @PathVariable Long id,
            @PathVariable Long setId,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã thêm bộ thẻ vào thư mục",
                folderService.addSetToFolder(id, setId, token, currentUser)
        ));
    }

    @DeleteMapping("/{id}/sets/{setId}")
    @Operation(summary = "Xóa bộ thẻ khỏi thư mục")
    public ResponseEntity<ApiResponse<FolderDetailResponse>> removeSetFromFolder(
            @PathVariable Long id,
            @PathVariable Long setId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xóa bộ thẻ khỏi thư mục",
                folderService.removeSetFromFolder(id, setId, currentUser)
        ));
    }
}
