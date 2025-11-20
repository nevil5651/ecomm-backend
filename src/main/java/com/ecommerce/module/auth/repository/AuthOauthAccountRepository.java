package com.ecommerce.module.auth.repository;

import com.ecommerce.module.auth.entity.AuthOauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthOauthAccountRepository extends JpaRepository<AuthOauthAccount, Long> {
    Optional<AuthOauthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<AuthOauthAccount> findByProviderAndEmail(String provider, String email);
}