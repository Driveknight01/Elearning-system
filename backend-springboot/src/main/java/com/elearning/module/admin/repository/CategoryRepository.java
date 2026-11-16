package com.elearning.module.admin.repository;

import com.elearning.module.admin.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByParentIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);
}
