package com.elearning.module.group.repository;

import com.elearning.module.group.entity.StudyGroupSet;
import com.elearning.module.group.entity.StudyGroupSetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyGroupSetRepository extends JpaRepository<StudyGroupSet, StudyGroupSetId> {

    boolean existsByIdGroupIdAndIdSetId(Long groupId, Long setId);

    List<StudyGroupSet> findAllByIdGroupIdOrderBySharedAtDesc(Long groupId);

    void deleteByIdGroupIdAndIdSetId(Long groupId, Long setId);
}
