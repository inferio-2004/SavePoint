package com.example.Savepoint.User;

import com.example.Savepoint.Auth.AuthProvider;
import com.example.Savepoint.Auth.UserAuth;
import com.example.Savepoint.Auth.UserAuthJpaRepositry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAuthJpaRepositry repositry;

    public CustomUserDetailsService(UserAuthJpaRepositry repositry) {
        this.repositry = repositry;
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String mail) {

         UserAuth foundedUserAuth = repositry.findByProviderAndMailId(AuthProvider.MANUAL, mail)
                 .orElseThrow(() -> new UsernameNotFoundException("User not found"));
         String passwordHash = foundedUserAuth.getPasswordHash();
         String username = foundedUserAuth.getUser().getDisplayName();

        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(passwordHash) // already BCrypt encoded
                .build();
    }
}
