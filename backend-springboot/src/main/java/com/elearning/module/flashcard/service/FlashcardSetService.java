package com.elearning.module.flashcard.service;

import com.elearning.common.dto.PageResponse;
import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.common.util.ShareTokenGenerator;
import com.elearning.module.admin.entity.Category;
import com.elearning.module.admin.repository.CategoryRepository;
import com.elearning.module.flashcard.dto.CreateFlashcardSetRequest;
import com.elearning.module.flashcard.dto.FlashcardItemRequest;
import com.elearning.module.flashcard.dto.FlashcardResponse;
import com.elearning.module.flashcard.dto.SetDetailResponse;
import com.elearning.module.flashcard.dto.SetSummaryResponse;
import com.elearning.module.flashcard.dto.UpdateFlashcardSetRequest;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.SourceType;
import com.elearning.module.flashcard.entity.Visibility;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Business logic for creating, reading, updating, and soft-deleting flashcard sets
 * with synchronous child card persistence and 3-level visibility access control.
 */
@Service
@RequiredArgsConstructor
public class FlashcardSetService {

    private final FlashcardSetRepository flashcardSetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public SetDetailResponse createSet(Long ownerId, CreateFlashcardSetRequest request) {
        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Category category = resolveCategory(request.getCategoryId());
        String shareToken = generateUniqueShareToken();

        FlashcardSet set = FlashcardSet.builder()
                .owner(owner)
                .category(category)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .moderationStatus(ModerationStatus.APPROVED)
                .shareToken(shareToken)
                .termLang(hasText(request.getTermLang()) ? request.getTermLang().trim() : "en")
                .defLang(hasText(request.getDefLang()) ? request.getDefLang().trim() : "vi")
                .tags(normalizeTags(request.getTags()))
                .sourceType(request.getSourceType() != null ? request.getSourceType() : SourceType.MANUAL)
                .build();

        List<FlashcardItemRequest> items = request.getCards();
        for (int i = 0; i < items.size(); i++) {
            FlashcardItemRequest item = items.get(i);
            Flashcard card = buildNewCard(set, item, i);
            set.addCard(card);
        }
        set.setCardCount(set.getCards().size());

        FlashcardSet saved = flashcardSetRepository.saveAndFlush(set);
        return toDetailResponse(saved, true);
    }

    @Transactional
    public SetDetailResponse getSetById(Long setId, String shareToken, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetRepository.findByIdAndDeletedAtIsNull(setId)
                .orElseThrow(() -> new AppException(ErrorCode.SET_NOT_FOUND));

        boolean isOwnerOrAdmin = isOwnerOrAdmin(set, currentUser);
        assertCanView(set, shareToken, isOwnerOrAdmin);

        if (!isOwnerOrAdmin) {
            set.setViewCount(set.getViewCount() + 1);
            flashcardSetRepository.save(set);
        }

        boolean isOwner = currentUser != null && set.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(set, isOwner);
    }

    @Transactional
    public SetDetailResponse getSetByShareToken(String shareToken, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetRepository.findByShareTokenAndDeletedAtIsNull(shareToken)
                .orElseThrow(() -> new AppException(ErrorCode.SET_NOT_FOUND));

        boolean isOwnerOrAdmin = isOwnerOrAdmin(set, currentUser);
        assertCanView(set, shareToken, isOwnerOrAdmin);

        if (!isOwnerOrAdmin) {
            set.setViewCount(set.getViewCount() + 1);
            flashcardSetRepository.save(set);
        }

        boolean isOwner = currentUser != null && set.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(set, isOwner);
    }

    @Transactional(readOnly = true)
    public PageResponse<SetSummaryResponse> getMySets(Long ownerId, Pageable pageable) {
        Page<FlashcardSet> page = flashcardSetRepository.findAllByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ownerId, pageable);
        List<SetSummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional
    public SetDetailResponse updateSet(Long setId, UserPrincipal currentUser, UpdateFlashcardSetRequest request) {
        FlashcardSet set = flashcardSetRepository.findByIdAndDeletedAtIsNull(setId)
                .orElseThrow(() -> new AppException(ErrorCode.SET_NOT_FOUND));

        if (!isOwnerOrAdmin(set, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền chỉnh sửa bộ thẻ này");
        }

        set.setTitle(request.getTitle().trim());
        set.setDescription(trimToNull(request.getDescription()));
        set.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getVisibility() != null) {
            set.setVisibility(request.getVisibility());
        }
        if (hasText(request.getTermLang())) {
            set.setTermLang(request.getTermLang().trim());
        }
        if (hasText(request.getDefLang())) {
            set.setDefLang(request.getDefLang().trim());
        }
        if (request.getTags() != null) {
            set.setTags(normalizeTags(request.getTags()));
        }
        if (request.isRegenerateShareToken()) {
            set.setShareToken(generateUniqueShareToken());
        }

        synchronizeCards(set, request.getCards());
        FlashcardSet saved = flashcardSetRepository.saveAndFlush(set);
        boolean isOwner = currentUser != null && set.getOwner().getId().equals(currentUser.getId());
        return toDetailResponse(saved, isOwner);
    }

    @Transactional
    public void deleteSet(Long setId, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetRepository.findByIdAndDeletedAtIsNull(setId)
                .orElseThrow(() -> new AppException(ErrorCode.SET_NOT_FOUND));

        if (!isOwnerOrAdmin(set, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa bộ thẻ này");
        }

        set.setDeletedAt(Instant.now());
        flashcardSetRepository.save(set);
    }

    private void synchronizeCards(FlashcardSet set, List<FlashcardItemRequest> items) {
        Map<Long, Flashcard> existingById = set.getCards().stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Flashcard::getId, Function.identity()));

        List<Flashcard> synchronizedCards = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FlashcardItemRequest item = items.get(i);
            int orderIndex = item.getOrderIndex() != null ? item.getOrderIndex() : i;
            Flashcard existing = item.getId() != null ? existingById.get(item.getId()) : null;

            if (existing != null) {
                existing.setTerm(item.getTerm().trim());
                existing.setDefinition(item.getDefinition().trim());
                existing.setHint(trimToNull(item.getHint()));
                existing.setImageUrl(trimToNull(item.getImageUrl()));
                existing.setAudioUrl(trimToNull(item.getAudioUrl()));
                existing.setConceptTag(trimToNull(item.getConceptTag()));
                existing.setOrderIndex(orderIndex);
                synchronizedCards.add(existing);
            } else {
                synchronizedCards.add(buildNewCard(set, item, orderIndex));
            }
        }

        set.getCards().clear();
        set.getCards().addAll(synchronizedCards);
        set.setCardCount(synchronizedCards.size());
    }

    private Flashcard buildNewCard(FlashcardSet set, FlashcardItemRequest item, int defaultOrderIndex) {
        return Flashcard.builder()
                .flashcardSet(set)
                .term(item.getTerm().trim())
                .definition(item.getDefinition().trim())
                .hint(trimToNull(item.getHint()))
                .imageUrl(trimToNull(item.getImageUrl()))
                .audioUrl(trimToNull(item.getAudioUrl()))
                .conceptTag(trimToNull(item.getConceptTag()))
                .orderIndex(item.getOrderIndex() != null ? item.getOrderIndex() : defaultOrderIndex)
                .build();
    }

    private void assertCanView(FlashcardSet set, String shareToken, boolean isOwnerOrAdmin) {
        if (isOwnerOrAdmin) {
            return;
        }
        if (set.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bộ thẻ này hiện không khả dụng");
        }
        switch (set.getVisibility()) {
            case PUBLIC -> {
                // Publicly accessible
            }
            case LINK_ONLY -> {
                if (!hasText(shareToken) || !shareToken.equals(set.getShareToken())) {
                    throw new AppException(ErrorCode.FORBIDDEN, "Cần liên kết chia sẻ hợp lệ để xem bộ thẻ này");
                }
            }
            case PRIVATE -> throw new AppException(ErrorCode.FORBIDDEN, "Bộ thẻ này đang ở chế độ riêng tư");
        }
    }

    private boolean isOwnerOrAdmin(FlashcardSet set, UserPrincipal currentUser) {
        if (currentUser == null) {
            return false;
        }
        return set.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole() == Role.ADMIN;
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy danh mục"));
    }

    private String generateUniqueShareToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = ShareTokenGenerator.generate();
            if (!flashcardSetRepository.existsByShareToken(candidate)) {
                return candidate;
            }
        }
        return ShareTokenGenerator.generate(16);
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new ArrayList<>();
        }
        return rawTags.stream()
                .filter(FlashcardSetService::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private SetSummaryResponse toSummaryResponse(FlashcardSet set) {
        return SetSummaryResponse.builder()
                .id(set.getId())
                .title(set.getTitle())
                .description(set.getDescription())
                .visibility(set.getVisibility())
                .moderationStatus(set.getModerationStatus())
                .featured(set.isFeatured())
                .shareToken(set.getShareToken())
                .termLang(set.getTermLang())
                .defLang(set.getDefLang())
                .tags(set.getTags())
                .sourceType(set.getSourceType())
                .cardCount(set.getCardCount())
                .viewCount(set.getViewCount())
                .cloneCount(set.getCloneCount())
                .avgRating(set.getAvgRating())
                .ratingCount(set.getRatingCount())
                .owner(toOwnerSummary(set.getOwner()))
                .category(toCategorySummary(set.getCategory()))
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    private SetDetailResponse toDetailResponse(FlashcardSet set, boolean isOwnerView) {
        List<FlashcardResponse> cardResponses = set.getCards().stream()
                .map(this::toCardResponse)
                .toList();

        return SetDetailResponse.builder()
                .id(set.getId())
                .title(set.getTitle())
                .description(set.getDescription())
                .visibility(set.getVisibility())
                .moderationStatus(set.getModerationStatus())
                .featured(set.isFeatured())
                .shareToken(set.getShareToken())
                .termLang(set.getTermLang())
                .defLang(set.getDefLang())
                .tags(set.getTags())
                .sourceType(set.getSourceType())
                .clonedFromSetId(set.getClonedFromSet() != null ? set.getClonedFromSet().getId() : null)
                .cardCount(set.getCardCount())
                .viewCount(set.getViewCount())
                .cloneCount(set.getCloneCount())
                .avgRating(set.getAvgRating())
                .ratingCount(set.getRatingCount())
                .ownerView(isOwnerView)
                .owner(toOwnerSummary(set.getOwner()))
                .category(toCategorySummary(set.getCategory()))
                .cards(cardResponses)
                .createdAt(set.getCreatedAt())
                .updatedAt(set.getUpdatedAt())
                .build();
    }

    private FlashcardResponse toCardResponse(Flashcard card) {
        return FlashcardResponse.builder()
                .id(card.getId())
                .term(card.getTerm())
                .definition(card.getDefinition())
                .hint(card.getHint())
                .imageUrl(card.getImageUrl())
                .audioUrl(card.getAudioUrl())
                .conceptTag(card.getConceptTag())
                .orderIndex(card.getOrderIndex())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    private SetSummaryResponse.OwnerSummary toOwnerSummary(User owner) {
        return SetSummaryResponse.OwnerSummary.builder()
                .id(owner.getId())
                .username(owner.getUsername())
                .fullName(owner.getFullName())
                .avatarUrl(owner.getAvatarUrl())
                .verifiedTeacher(owner.isVerifiedTeacher())
                .build();
    }

    private SetSummaryResponse.CategorySummary toCategorySummary(Category category) {
        if (category == null) {
            return null;
        }
        return SetSummaryResponse.CategorySummary.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .colorHex(category.getColorHex())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
