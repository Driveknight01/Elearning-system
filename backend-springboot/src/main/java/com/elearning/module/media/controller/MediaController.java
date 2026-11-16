package com.elearning.module.media.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.module.media.dto.AiImageGenerateRequest;
import com.elearning.module.media.dto.AiImageGenerateResponse;
import com.elearning.module.media.dto.MediaUploadResponse;
import com.elearning.module.media.dto.StockImageSearchResponse;
import com.elearning.module.media.service.MediaService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for Flashcard card image upload, free stock illustration search,
 * and AI mnemonic image generation.
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media & Card Illustrations", description = "Upload ảnh thẻ, tìm ảnh minh họa miễn phí (Pixabay/Unsplash) và tạo ảnh AI (Pollinations)")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh minh họa thẻ Flashcard lên Local Storage (/uploads/cards/**)")
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadCardImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cardId", required = false) Long cardId
    ) {
        MediaUploadResponse response = mediaService.uploadCardImage(file, cardId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh thẻ lên thành công", response));
    }

    @GetMapping("/stock-search")
    @Operation(summary = "Tìm kiếm ảnh minh họa miễn phí theo từ khóa Term (Pixabay / Unsplash / Fallback)")
    public ResponseEntity<ApiResponse<StockImageSearchResponse>> searchStockImages(
            @RequestParam String query,
            @RequestParam(defaultValue = "12") int perPage
    ) {
        StockImageSearchResponse response = mediaService.searchStockImages(query, perPage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/ai-generate")
    @Operation(summary = "Tạo ảnh minh họa tự động bằng AI (Pollinations) và tùy chọn lưu URL vào thẻ")
    public ResponseEntity<ApiResponse<AiImageGenerateResponse>> generateAiMnemonicImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AiImageGenerateRequest request
    ) {
        AiImageGenerateResponse response = mediaService.generateAiMnemonicImage(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Tạo ảnh minh họa AI thành công", response));
    }
}
