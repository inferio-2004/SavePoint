package com.example.Savepoint.Auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;
import java.util.Map;

public class SteamAuthToken extends AbstractAuthenticationToken {
    private final Map<String, String> openIdParams;
    private final String principal;

    // Unauthenticated constructor - used in controller before verification
    public SteamAuthToken(Map<String, String> openIdParams) {
        super(Collections.emptyList());
        this.openIdParams = openIdParams;
        this.principal = null;
        setAuthenticated(false);
    }

    // Authenticated constructor - used in provider after verification
    public SteamAuthToken(Map<String, String> openIdParams, String principal) {
        super(Collections.emptyList());
        this.openIdParams = openIdParams;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return openIdParams;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}