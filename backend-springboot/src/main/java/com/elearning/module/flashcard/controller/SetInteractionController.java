package com.elearning.module.flashcard.controller;

import com.elearning.common.dto.ApiResponse;
import com.elearning.common.dto.PageResponse;
import com.elearning.module.flashcard.dto.RateSetRequest;
import com.elearning.module.flashcard.dto.ResumeStateRequest;
import com.elearning.module.flashcard.dto.SetInteractionResponse;
import com.elearning.module.flashcard.dto.SetRatingItemResponse;
import com.elearning.module.flashcard.dto.StarredCardsResponse;
import com.elearning.module.flashcard.service.SetInteractionService;
import com.elearning.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * REST endpoints for set interactions (Save/Bookmark, Recents, Rating, Resume State)
 * and per-card starring ({@code starred_cards}).
 */
@RestController
@RequestMapping("/api/v1/flashcard-sets")
@RequiredArgsConstructor
@Tag(name = "Set Interactions & Starred Cards", description = "Lưu bộ thẻ, Học gần đây, Đánh giá sao, Đồng bộ vị trí học dở và Gắn sao thẻ khó")
public class SetInteractionController {

    private final SetInteractionService setInteractionService;

    @PostMapping("/{id}/save")
    @Operation(summary = "Lưu (Bookmark) bộ thẻ vào thư viện cá nhân")
    public ResponseEntity<ApiResponse<SetInteractionResponse>> saveSet(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã lưu bộ thẻ",
                setInteractionService.setSavedStatus(id, token, currentUser, true)
        ));
    }

    @DeleteMapping("/{id}/save")
    @Operation(summary = "Bỏ lưu (Unbookmark) bộ thẻ khỏi thư viện cá nhân")
    public ResponseEntity<ApiResponse<SetInteractionResponse>> unsaveSet(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã bỏ lưu bộ thẻ",
                setInteractionService.setSavedStatus(id, token, currentUser, false)
        ));
    }

    @GetMapping("/saved")
    @Operation(summary = "Lấy danh sách bộ thẻ đã lưu của người dùng hiện tại")
    public ResponseEntity<ApiResponse<PageResponse<SetInteractionResponse>>> getSavedSets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.success(setInteractionService.getSavedSets(currentUser.getId(), pageable)));
    }

    @GetMapping("/recents")
    @Operation(summary = "Lấy danh sách bộ thẻ học gần đây (Recents)")
    public ResponseEntity<ApiResponse<PageResponse<SetInteractionResponse>>> getRecentSets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.success(setInteractionService.getRecentSets(currentUser.getId(), pageable)));
    }

    @GetMapping("/{id}/interaction")
    @Operation(summary = "Lấy trạng thái tương tác & vị trí học dở hiện tại của người dùng với bộ thẻ")
    public ResponseEntity<ApiResponse<SetInteractionResponse>> getInteraction(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(setInteractionService.getInteraction(id, token, currentUser)));
    }

    @PutMapping("/{id}/resume")
    @Operation(summary = "Lưu trạng thái học dở (resume_state_json) để đồng bộ giữa Web và Android")
    public ResponseEntity<ApiResponse<SetInteractionResponse>> saveResumeState(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ResumeStateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đồng bộ tiến độ phiên học",
                setInteractionService.saveResumeState(id, token, currentUser, request)
        ));
    }

    @PostMapping("/{id}/rate")
    @Operation(summary = "Đánh giá 1-5 sao và nhận xét cho bộ thẻ")
    public ResponseEntity<ApiResponse<SetInteractionResponse>> rateSet(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RateSetRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đánh giá bộ thẻ thành công",
                setInteractionService.rateSet(id, token, currentUser, request)
        ));
    }

    @GetMapping("/{id}/ratings")
    @Operation(summary = "Xem danh sách đánh giá sao của bộ thẻ")
    public ResponseEntity<ApiResponse<PageResponse<SetRatingItemResponse>>> getSetRatings(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.success(
                setInteractionService.getSetRatings(id, token, currentUser, pageable)
        ));
    }

    @PostMapping("/{id}/cards/{cardId}/star")
    @Operation(summary = "Gắn dấu sao (★) cho một thẻ khó")
    public ResponseEntity<ApiResponse<Void>> starCard(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        setInteractionService.starCard(id, cardId, token, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Đã gắn dấu sao thẻ"));
    }

    @DeleteMapping("/{id}/cards/{cardId}/star")
    @Operation(summary = "Bỏ gắn dấu sao thẻ")
    public ResponseEntity<ApiResponse<Void>> unstarCard(
            @PathVariable Long id,
            @PathVariable Long cardId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        setInteractionService.unstarCard(id, cardId, currentUser);
        return ResponseEntity.ok(ApiResponse.successMessage("Đã bỏ gắn dấu sao thẻ"));
    }

    @GetMapping("/{id}/starred-cards")
    @Operation(summary = "Lấy danh sách thẻ đã gắn dấu sao của người dùng trong bộ thẻ")
    public ResponseEntity<ApiResponse<StarredCardsResponse>> getStarredCards(
            @PathVariable Long id,
            @RequestParam(required = false) String token,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(setInteractionService.getStarredCards(id, token, currentUser)));
    }
}
