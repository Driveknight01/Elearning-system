package com.elearning.module.flashcard.service;

import com.elearning.common.dto.PageResponse;
import com.elearning.common.exception.AppException;
import com.elearning.common.exception.ErrorCode;
import com.elearning.module.flashcard.dto.FlashcardResponse;
import com.elearning.module.flashcard.dto.RateSetRequest;
import com.elearning.module.flashcard.dto.ResumeStateRequest;
import com.elearning.module.flashcard.dto.SetInteractionResponse;
import com.elearning.module.flashcard.dto.SetRatingItemResponse;
import com.elearning.module.flashcard.dto.StarredCardsResponse;
import com.elearning.module.flashcard.entity.Flashcard;
import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.StarredCard;
import com.elearning.module.flashcard.entity.StarredCardId;
import com.elearning.module.flashcard.entity.UserSetInteraction;
import com.elearning.module.flashcard.entity.UserSetInteractionId;
import com.elearning.module.flashcard.repository.FlashcardRepository;
import com.elearning.module.flashcard.repository.FlashcardSetRepository;
import com.elearning.module.flashcard.repository.StarredCardRepository;
import com.elearning.module.flashcard.repository.UserSetInteractionRepository;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.repository.UserRepository;
import com.elearning.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

/**
 * Handles user-set interactions (Saved, Recents, 1-5 star ratings, cross-platform resume state)
 * and per-card bookmarking ({@code starred_cards}).
 */
@Service
@RequiredArgsConstructor
public class SetInteractionService {

    private final UserSetInteractionRepository userSetInteractionRepository;
    private final StarredCardRepository starredCardRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final FlashcardSetService flashcardSetService;

    @Transactional
    public SetInteractionResponse setSavedStatus(Long setId, String shareToken, UserPrincipal currentUser, boolean saved) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        UserSetInteraction interaction = getOrCreateInteraction(currentUser.getId(), set);
        interaction.setSaved(saved);
        interaction.setLastAccessedAt(Instant.now());
        UserSetInteraction persisted = userSetInteractionRepository.saveAndFlush(interaction);
        return toInteractionResponse(persisted, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SetInteractionResponse> getSavedSets(Long userId, Pageable pageable) {
        Page<UserSetInteraction> page = userSetInteractionRepository
                .findAllByIdUserIdAndSavedTrueAndFlashcardSetDeletedAtIsNullOrderByLastAccessedAtDesc(userId, pageable);
        List<SetInteractionResponse> content = page.getContent().stream()
                .map(i -> toInteractionResponse(i, true))
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<SetInteractionResponse> getRecentSets(Long userId, Pageable pageable) {
        Page<UserSetInteraction> page = userSetInteractionRepository
                .findAllByIdUserIdAndFlashcardSetDeletedAtIsNullOrderByLastAccessedAtDesc(userId, pageable);
        List<SetInteractionResponse> content = page.getContent().stream()
                .map(i -> toInteractionResponse(i, true))
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public SetInteractionResponse getInteraction(Long setId, String shareToken, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        return userSetInteractionRepository.findByIdUserIdAndIdSetId(currentUser.getId(), setId)
                .map(i -> toInteractionResponse(i, false))
                .orElseGet(() -> SetInteractionResponse.builder()
                        .setId(setId)
                        .saved(false)
                        .resumeStateJson(new HashMap<>())
                        .build());
    }

    @Transactional
    public SetInteractionResponse saveResumeState(
            Long setId,
            String shareToken,
            UserPrincipal currentUser,
            ResumeStateRequest request
    ) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        UserSetInteraction interaction = getOrCreateInteraction(currentUser.getId(), set);
        interaction.setLastStudyMode(request.getStudyMode());
        interaction.setLastAccessedAt(Instant.now());
        interaction.setResumeStateJson(
                request.getResumeStateJson() != null ? new HashMap<>(request.getResumeStateJson()) : new HashMap<>()
        );
        UserSetInteraction persisted = userSetInteractionRepository.saveAndFlush(interaction);
        return toInteractionResponse(persisted, false);
    }

    @Transactional
    public SetInteractionResponse rateSet(
            Long setId,
            String shareToken,
            UserPrincipal currentUser,
            RateSetRequest request
    ) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        UserSetInteraction interaction = getOrCreateInteraction(currentUser.getId(), set);
        interaction.setRating(request.getRating());
        interaction.setReviewComment(trimToNull(request.getReviewComment()));
        interaction.setRatedAt(Instant.now());
        interaction.setLastAccessedAt(Instant.now());
        UserSetInteraction persisted = userSetInteractionRepository.saveAndFlush(interaction);

        Double avg = userSetInteractionRepository.findAverageRatingBySetId(setId);
        long count = userSetInteractionRepository.countByIdSetIdAndRatingIsNotNull(setId);
        set.setAvgRating(avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : new BigDecimal("0.00"));
        set.setRatingCount((int) count);
        flashcardSetRepository.save(set);

        return toInteractionResponse(persisted, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<SetRatingItemResponse> getSetRatings(
            Long setId,
            String shareToken,
            UserPrincipal currentUser,
            Pageable pageable
    ) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        Page<UserSetInteraction> page = userSetInteractionRepository
                .findAllByIdSetIdAndRatingIsNotNullOrderByRatedAtDesc(setId, pageable);

        List<SetRatingItemResponse> content = page.getContent().stream()
                .map(i -> SetRatingItemResponse.builder()
                        .userId(i.getUser().getId())
                        .username(i.getUser().getUsername())
                        .fullName(i.getUser().getFullName())
                        .avatarUrl(i.getUser().getAvatarUrl())
                        .rating(i.getRating())
                        .reviewComment(i.getReviewComment())
                        .ratedAt(i.getRatedAt())
                        .build())
                .toList();
        return PageResponse.from(page, content);
    }

    @Transactional
    public void starCard(Long setId, Long cardId, String shareToken, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        Flashcard card = flashcardRepository.findByIdAndFlashcardSetId(cardId, setId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thẻ trong bộ thẻ này"));

        if (!starredCardRepository.existsByIdUserIdAndIdCardId(currentUser.getId(), cardId)) {
            User user = userRepository.getReferenceById(currentUser.getId());
            starredCardRepository.save(StarredCard.builder()
                    .id(new StarredCardId(currentUser.getId(), cardId))
                    .user(user)
                    .flashcard(card)
                    .build());
        }
    }

    @Transactional
    public void unstarCard(Long setId, Long cardId, UserPrincipal currentUser) {
        flashcardSetService.findActiveSetOrThrow(setId);
        starredCardRepository.deleteByIdUserIdAndIdCardId(currentUser.getId(), cardId);
    }

    @Transactional(readOnly = true)
    public StarredCardsResponse getStarredCards(Long setId, String shareToken, UserPrincipal currentUser) {
        FlashcardSet set = flashcardSetService.findActiveSetOrThrow(setId);
        flashcardSetService.assertCanView(set, shareToken, flashcardSetService.isOwnerOrAdmin(set, currentUser));

        List<StarredCard> starred = starredCardRepository.findAllByIdUserIdAndFlashcardFlashcardSetId(currentUser.getId(), setId);
        List<Long> cardIds = starred.stream().map(s -> s.getFlashcard().getId()).toList();
        List<FlashcardResponse> cards = starred.stream()
                .map(s -> flashcardSetService.toCardResponse(s.getFlashcard()))
                .toList();

        return StarredCardsResponse.builder()
                .setId(setId)
                .starredCardIds(cardIds)
                .cards(cards)
                .build();
    }

    private UserSetInteraction getOrCreateInteraction(Long userId, FlashcardSet set) {
        return userSetInteractionRepository.findByIdUserIdAndIdSetId(userId, set.getId())
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return UserSetInteraction.builder()
                            .id(new UserSetInteractionId(userId, set.getId()))
                            .user(user)
                            .flashcardSet(set)
                            .build();
                });
    }

    private SetInteractionResponse toInteractionResponse(UserSetInteraction interaction, boolean includeSetSummary) {
        return SetInteractionResponse.builder()
                .setId(interaction.getId().getSetId())
                .saved(interaction.isSaved())
                .lastStudyMode(interaction.getLastStudyMode())
                .lastAccessedAt(interaction.getLastAccessedAt())
                .resumeStateJson(interaction.getResumeStateJson())
                .rating(interaction.getRating())
                .reviewComment(interaction.getReviewComment())
                .ratedAt(interaction.getRatedAt())
                .set(includeSetSummary ? flashcardSetService.toSummaryResponse(interaction.getFlashcardSet()) : null)
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
