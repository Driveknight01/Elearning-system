package com.elearning.module.auth.repository;

import com.elearning.module.auth.entity.TokenType;
import com.elearning.module.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findTopByUserIdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
        Long userId,
        TokenType tokenType
    );

    Optional<VerificationToken> findByUserIdAndTokenCodeAndTokenTypeAndUsedAtIsNull(
        Long userId,
        String tokenCode,
        TokenType tokenType
    );
}
