package com.elearning.module.auth.repository;

import com.elearning.module.auth.entity.OAuthAccount;
import com.elearning.module.auth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    List<OAuthAccount> findAllByUserId(Long userId);
}
