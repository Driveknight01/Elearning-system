package com.elearning.module.group.repository;

import com.elearning.module.group.entity.StudyGroupMember;
import com.elearning.module.group.entity.StudyGroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, StudyGroupMemberId> {

    boolean existsByIdGroupIdAndIdUserId(Long groupId, Long userId);

    Optional<StudyGroupMember> findByIdGroupIdAndIdUserId(Long groupId, Long userId);

    List<StudyGroupMember> findAllByIdGroupIdOrderByJoinedAtAsc(Long groupId);

    long countByIdGroupId(Long groupId);

    void deleteByIdGroupIdAndIdUserId(Long groupId, Long userId);
}
