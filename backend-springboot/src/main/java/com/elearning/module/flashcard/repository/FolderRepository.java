package com.elearning.module.flashcard.repository;

import com.elearning.module.flashcard.entity.Folder;
import com.elearning.module.flashcard.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByShareToken(String shareToken);

    boolean existsByShareToken(String shareToken);

    Page<Folder> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    List<Folder> findAllByOwnerIdAndVisibilityOrderByUpdatedAtDesc(Long ownerId, Visibility visibility);
}
