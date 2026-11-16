package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findAllByFlashcardSetIdOrderByOrderIndexAsc(Long setId);

    Optional<Flashcard> findByIdAndFlashcardSetId(Long id, Long flashcardSetId);

    long countByFlashcardSetId(Long setId);

    void deleteAllByFlashcardSetId(Long setId);
}
