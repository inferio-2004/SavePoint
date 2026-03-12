package com.example.Savepoint.Auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAuthJpaRepositry extends JpaRepository<UserAuth, Integer> {
    Optional<UserAuth> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<UserAuth> findByProviderAndMailId(AuthProvider provider,String mailId);
}