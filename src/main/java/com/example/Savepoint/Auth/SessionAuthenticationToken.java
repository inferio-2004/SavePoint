package com.example.Savepoint.Auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class SessionAuthenticationToken extends AbstractAuthenticationToken {
    private final SessionAuthPrincipal principal;

    public SessionAuthenticationToken(SessionAuthPrincipal principal) {
        this(principal, Collections.emptyList());
    }

    public SessionAuthenticationToken(SessionAuthPrincipal principal,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public SessionAuthPrincipal getPrincipal() {
        return principal;
    }

}

