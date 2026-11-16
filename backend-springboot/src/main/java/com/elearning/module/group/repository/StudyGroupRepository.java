package com.elearning.module.group.repository;

import com.elearning.module.group.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    Optional<StudyGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<StudyGroup> findAllByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    @Query("SELECT g FROM StudyGroup g JOIN g.members m WHERE m.id.userId = :userId ORDER BY g.createdAt DESC")
    List<StudyGroup> findAllByMemberUserId(@Param("userId") Long userId);
}
