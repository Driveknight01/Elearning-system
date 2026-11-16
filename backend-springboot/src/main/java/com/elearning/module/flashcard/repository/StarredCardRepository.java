package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.StarredCard;
import com.elearning.module.flashcard.entity.StarredCardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StarredCardRepository extends JpaRepository<StarredCard, StarredCardId> {

    boolean existsByIdUserIdAndIdCardId(Long userId, Long cardId);

    List<StarredCard> findAllByIdUserIdAndFlashcardFlashcardSetId(Long userId, Long setId);

    void deleteByIdUserIdAndIdCardId(Long userId, Long cardId);

    long countByIdUserIdAndFlashcardFlashcardSetId(Long userId, Long setId);
}
