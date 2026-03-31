package com.example.Savepoint.Auth;

import com.example.Savepoint.User.UserRole;
import jakarta.annotation.Nullable;

import java.io.Serializable;

public record SessionAuthPrincipal(Integer id, String username, AuthProvider provider, UserRole role,@Nullable String providerId)
        implements Serializable {
}