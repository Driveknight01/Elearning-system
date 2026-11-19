package com.elearning.module.user.repository;

import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    Page<User> findAllByRoleAndDeletedAtIsNull(Role role, Pageable pageable);

    Page<User> findAllByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND u.status = com.elearning.module.user.entity.UserStatus.ACTIVE
              AND u.profileVisibility <> com.elearning.module.user.entity.ProfileVisibility.PRIVATE
              AND (
                    LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY u.username ASC
            """)
    Page<User> searchPublicUsers(@Param("keyword") String keyword, Pageable pageable);
}
