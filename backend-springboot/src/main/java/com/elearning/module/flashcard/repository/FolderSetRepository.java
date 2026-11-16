package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.FolderSet;
import com.elearning.module.flashcard.entity.FolderSetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderSetRepository extends JpaRepository<FolderSet, FolderSetId> {

    boolean existsByIdFolderIdAndIdSetId(Long folderId, Long setId);

    List<FolderSet> findAllByIdFolderIdOrderByAddedAtDesc(Long folderId);

    void deleteByIdFolderIdAndIdSetId(Long folderId, Long setId);

    long countByIdFolderId(Long folderId);
}
