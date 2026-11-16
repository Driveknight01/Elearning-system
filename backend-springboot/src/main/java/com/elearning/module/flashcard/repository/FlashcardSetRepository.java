package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.flashcard.entity.ModerationStatus;
import com.elearning.module.flashcard.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, Long> {

    Optional<FlashcardSet> findByIdAndDeletedAtIsNull(Long id);

    Optional<FlashcardSet> findByShareTokenAndDeletedAtIsNull(String shareToken);

    boolean existsByShareToken(String shareToken);

    Page<FlashcardSet> findAllByOwnerIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    Page<FlashcardSet> findAllByVisibilityAndModerationStatusAndDeletedAtIsNull(
            Visibility visibility,
            ModerationStatus moderationStatus,
            Pageable pageable
    );

    Page<FlashcardSet> findAllByCategoryIdAndVisibilityAndModerationStatusAndDeletedAtIsNull(
            Long categoryId,
            Visibility visibility,
            ModerationStatus moderationStatus,
            Pageable pageable
    );

    List<FlashcardSet> findAllByFeaturedTrueAndVisibilityAndModerationStatusAndDeletedAtIsNullOrderByViewCountDesc(
            Visibility visibility,
            ModerationStatus moderationStatus,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM FlashcardSet s
            WHERE s.deletedAt IS NULL
              AND s.visibility = :visibility
              AND s.moderationStatus = :moderationStatus
              AND (:categoryId IS NULL OR s.category.id = :categoryId)
              AND (
                    LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<FlashcardSet> searchPublicSets(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("visibility") Visibility visibility,
            @Param("moderationStatus") ModerationStatus moderationStatus,
            Pageable pageable
    );
}
