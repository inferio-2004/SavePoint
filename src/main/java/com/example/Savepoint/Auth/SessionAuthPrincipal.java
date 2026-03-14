package com.example.Savepoint.Auth;

import jakarta.annotation.Nullable;

import java.io.Serializable;

public record SessionAuthPrincipal(Integer id, String username, AuthProvider provider, @Nullable String providerId)
        implements Serializable {
}