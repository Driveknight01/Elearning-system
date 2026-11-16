package com.elearning.module.flashcard.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.common.dto.PageResponse;
import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.flashcard.dto.CloneSetRequest;
import com.elearning.module.flashcard.dto.CreateFlashcardSetRequest;
import com.elearning.module.flashcard.dto.ExploreSetsResponse;
import com.elearning.module.flashcard.dto.OcrExtractResponse;
import com.elearning.module.flashcard.dto.SetDetailResponse;
import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.flashcard.dto.UpdateFlashcardSetRequest;
import com.elearning.module.flashcard.service.AiOcrClient;
import com.elearning.module.flashcard.service.FlashcardSetService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * REST endpoints for Flashcard Set CRUD and visibility-guarded retrieval.
 */
@RestController
@RequestMapping("/api/v1/flashcard-sets")
@RequiredArgsConstructor
@Tag(name = "Flashcard Sets", description = "CRUD bộ thẻ Flashcard và kiểm soát quyền truy cập PUBLIC / PRIVATE / LINK_ONLY")
public class FlashcardSetController {

    private static final long MAX_OCR_IMAGE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_OCR_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/bmp", "image/tiff"
    );
    private static final Set<String> ALLOWED_OCR_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".tiff"
    );

    private final FlashcardSetService flashcardSetService;
    private final AiOcrClient aiOcrClient;

    @PostMapping
    @Operation(summary = "Tạo bộ thẻ mới kèm danh sách thẻ con đồng bộ")
    public ResponseEntity<ApiResponse<SetDetailResponse>> createSet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateFlashcardSetRequest request
    ) {
        SetDetailResponse created = flashcardSetService.createSet(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bộ thẻ thành công", created));
    }

    @GetMapping("/explore")
    @Operation(summary = "Khám phá bộ thẻ nổi bật, phổ biến, mới nhất và danh mục gốc (Guest + User)")
    public ResponseEntity<ApiResponse<ExploreSetsResponse>> getExploreSets() {
        return ResponseEntity.ok(ApiResponse.success(flashcardSetService.getExploreSets()));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm & lọc bộ thẻ PUBLIC theo từ khóa, danh mục và sắp xếp (Guest + User)")
    public ResponseEntity<ApiResponse<PageResponse<SetSummaryResponse>>> searchPublicSets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                flashcardSetService.searchPublicSets(keyword, categoryId, sort, page, size)
        ));
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách bộ thẻ của người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<PageResponse<SetSummaryResponse>>> getMySets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.success(flashcardSetService.getMySets(currentUser.getId(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết bộ thẻ (kiểm tra quyền PUBLIC / LINK_ONLY?token= / PRIVATE)")
    public ResponseEntity<ApiResponse<SetDetailResponse>> getSetById(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(flashcardSetService.getSetById(id, token, currentUser)));
    }

    @GetMapping("/share/{shareToken}")
    @Operation(summary = "Truy cập bộ thẻ qua mã chia sẻ share_token")
    public ResponseEntity<ApiResponse<SetDetailResponse>> getSetByShareToken(
            @PathVariable String shareToken,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(flashcardSetService.getSetByShareToken(shareToken, currentUser)));
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Nhân bản (Clone) bộ thẻ sang tài khoản cá nhân")
    public ResponseEntity<ApiResponse<SetDetailResponse>> cloneSet(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody(required = false) CloneSetRequest request
    ) {
        SetDetailResponse cloned = flashcardSetService.cloneSet(id, token, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nhân bản bộ thẻ thành công", cloned));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bộ thẻ và đồng bộ danh sách thẻ con")
    public ResponseEntity<ApiResponse<SetDetailResponse>> updateSet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateFlashcardSetRequest request
    ) {
        SetDetailResponse updated = flashcardSetService.updateSet(id, currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bộ thẻ thành công", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm bộ thẻ")
    public ResponseEntity<ApiResponse<Void>> deleteSet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        flashcardSetService.deleteSet(id, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Xóa bộ thẻ thành công"));
    }

    @PostMapping(value = "/ocr-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Trích xuất danh sách cặp thẻ Flashcard từ ảnh tài liệu qua AI OCR Service (không tự lưu DB)")
    public ResponseEntity<ApiResponse<OcrExtractResponse>> extractCardsFromOcrImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceLang", required = false) String sourceLang,
            @RequestParam(value = "targetLang", required = false) String targetLang
    ) {
        validateOcrImageFile(file);
        OcrExtractResponse result = aiOcrClient.extractCards(file, sourceLang, targetLang);
        return ResponseEntity.ok(ApiResponse.success("Trích xuất thẻ từ ảnh thành công", result));
    }

    private void validateOcrImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn tệp ảnh để quét OCR");
        }
        if (file.getSize() > MAX_OCR_IMAGE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Ảnh quét OCR tối đa 10MB");
        }
        String contentType = file.getContentType() != null ? file.getContentType().trim().toLowerCase() : "";
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().trim().toLowerCase() : "";
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";

        boolean validMime = ALLOWED_OCR_MIME_TYPES.contains(contentType);
        boolean validExtFallback = (!StringUtils.hasText(contentType) || "application/octet-stream".equals(contentType))
                && ALLOWED_OCR_EXTENSIONS.contains(ext);

        if (!validMime && !validExtFallback) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Chỉ hỗ trợ định dạng ảnh JPG, PNG, WEBP, BMP hoặc TIFF cho quét OCR"
            );
        }
    }
}
