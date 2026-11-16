package com.elearning.module.user.repository;

import com.elearning.module.user.entity.Role;
import com.elearning.module.user.entity.User;
import com.elearning.module.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);

    Page<User> findAllByRoleAndDeletedAtIsNull(Role role, Pageable pageable);

    Page<User> findAllByStatusAndDeletedAtIsNull(UserStatus status, Pageable pageable);
}
