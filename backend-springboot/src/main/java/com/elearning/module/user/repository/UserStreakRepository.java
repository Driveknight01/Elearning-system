package com.elearning.module.user.repository;

import com.elearning.module.user.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
}
