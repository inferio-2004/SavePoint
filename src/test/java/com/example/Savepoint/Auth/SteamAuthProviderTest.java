package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.InvalidSteamCredentialException;
import com.example.Savepoint.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamAuthProviderTest {

    @Mock RestTemplate restTemplate;
    @Mock UserService userService;

    private SteamAuthProvider steamAuthProvider;

    private static final String STEAM_ID = "76561198000000001";

    @BeforeEach
    void setup() {
        steamAuthProvider = new SteamAuthProvider(restTemplate, userService);
    }

    private Map<String, String> validOpenIdParams() {
        String claimedId = "https://steamcommunity.com/openid/id/" + STEAM_ID;
        return Map.of(
                "openid.claimed_id", claimedId,
                "openid.mode", "id_res",
                "openid.ns", "http://specs.openid.net/auth/2.0"
        );
    }

    private void mockSteamVerifySuccess() {
        ResponseEntity<String> verifyResponse = ResponseEntity.ok("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n");
        when(restTemplate.postForEntity(eq("https://steamcommunity.com/openid/login"), any(), eq(String.class)))
                .thenReturn(verifyResponse);
    }

    private void mockSteamVerifyFailure() {
        ResponseEntity<String> verifyResponse = ResponseEntity.ok("ns:http://specs.openid.net/auth/2.0\nis_valid:false\n");
        when(restTemplate.postForEntity(eq("https://steamcommunity.com/openid/login"), any(), eq(String.class)))
                .thenReturn(verifyResponse);
    }

    // ── supports() ───────────────────────────────────────────────────────────

    @Test
    void supports_trueForSteamAuthToken() {
        assertThat(steamAuthProvider.supports(SteamAuthToken.class)).isTrue();
    }

    @Test
    void supports_falseForOtherToken() {
        assertThat(steamAuthProvider.supports(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    // ── authenticate() — existing user ───────────────────────────────────────

    @Test
    void authenticate_returnsAuthenticatedToken_withSteamIdPrincipal() {
        mockSteamVerifySuccess();

        SteamAuthToken result = (SteamAuthToken) steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams()));

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(STEAM_ID);
    }

    @Test
    void authenticate_verifiesOpenIdResponseWithSteam() {
        mockSteamVerifySuccess();

        steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams()));

        verify(restTemplate).postForEntity(eq("https://steamcommunity.com/openid/login"), any(), eq(String.class));
    }

    @Test
    void authenticate_throwsInvalidSteamCredential_whenVerificationFails() {
        mockSteamVerifyFailure();

        assertThatThrownBy(() -> steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams())))
                .isInstanceOf(InvalidSteamCredentialException.class);
    }
}
