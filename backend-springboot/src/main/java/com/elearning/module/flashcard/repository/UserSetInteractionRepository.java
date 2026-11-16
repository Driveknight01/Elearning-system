package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.UserSetInteraction;
import com.elearning.module.flashcard.entity.UserSetInteractionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSetInteractionRepository extends JpaRepository<UserSetInteraction, UserSetInteractionId> {

    Optional<UserSetInteraction> findByIdUserIdAndIdSetId(Long userId, Long setId);

    Page<UserSetInteraction> findAllByIdUserIdAndSavedTrueAndFlashcardSetDeletedAtIsNullOrderByLastAccessedAtDesc(
            Long userId,
            Pageable pageable
    );

    Page<UserSetInteraction> findAllByIdUserIdAndFlashcardSetDeletedAtIsNullOrderByLastAccessedAtDesc(
            Long userId,
            Pageable pageable
    );

    Page<UserSetInteraction> findAllByIdUserIdAndSavedTrueOrderByLastAccessedAtDesc(Long userId, Pageable pageable);

    Page<UserSetInteraction> findAllByIdUserIdOrderByLastAccessedAtDesc(Long userId, Pageable pageable);

    Page<UserSetInteraction> findAllByIdSetIdAndRatingIsNotNullOrderByRatedAtDesc(Long setId, Pageable pageable);

    @Query("SELECT AVG(u.rating) FROM UserSetInteraction u WHERE u.id.setId = :setId AND u.rating IS NOT NULL")
    Double findAverageRatingBySetId(@Param("setId") Long setId);

    long countByIdSetIdAndRatingIsNotNull(Long setId);
}
