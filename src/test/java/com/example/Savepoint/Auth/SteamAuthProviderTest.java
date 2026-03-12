package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.InvalidSteamCredentialException;
import com.example.Savepoint.User.UserProfileDTO;
import com.example.Savepoint.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamAuthProviderTest {

    @Mock RestTemplate restTemplate;
    @Mock UserService userService;

    @InjectMocks SteamAuthProvider steamAuthProvider;

    private final String STEAM_ID = "76561198000000001";
    private final UserProfileDTO mockUser = new UserProfileDTO(1, "SteamUser", "http://avatar.url");

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(steamAuthProvider, "steamApiKey", "FAKE_API_KEY");
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
    void authenticate_returnsAuthenticatedToken_forExistingUser() {
        mockSteamVerifySuccess();
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.of(mockUser));

        SteamAuthToken token = new SteamAuthToken(validOpenIdParams());
        SteamAuthToken result = (SteamAuthToken) steamAuthProvider.authenticate(token);

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(mockUser);
    }

    @Test
    void authenticate_doesNotCallSteamProfileApi_forExistingUser() {
        mockSteamVerifySuccess();
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.of(mockUser));

        steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams()));

        // getForEntity (profile API) should never be called if user already exists
        verify(restTemplate, never()).getForEntity(anyString(), eq(SteamApiResponse.class));
    }

    // ── authenticate() — new user ─────────────────────────────────────────────

    @Test
    void authenticate_createsNewUser_whenNotFound() {
        mockSteamVerifySuccess();
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());

        SteamApiResponse apiResponse = new SteamApiResponse(
                new PlayersResponse(java.util.List.of(
                        new PlayerSummary(STEAM_ID, "NewSteamUser", "http://new-avatar.url")
                ))
        );
        when(restTemplate.getForEntity(anyString(), eq(SteamApiResponse.class)))
                .thenReturn(ResponseEntity.ok(apiResponse));
        when(userService.createSteamUser(STEAM_ID, "NewSteamUser", "http://new-avatar.url"))
                .thenReturn(new UserProfileDTO(2, "NewSteamUser", "http://new-avatar.url"));

        SteamAuthToken result = (SteamAuthToken) steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams()));

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(((UserProfileDTO) result.getPrincipal()).displayName()).isEqualTo("NewSteamUser");
        verify(userService).createSteamUser(STEAM_ID, "NewSteamUser", "http://new-avatar.url");
    }

    // ── authenticate() — Steam says invalid ──────────────────────────────────

    @Test
    void authenticate_throwsInvalidSteamCredential_whenVerificationFails() {
        mockSteamVerifyFailure();

        assertThatThrownBy(() -> steamAuthProvider.authenticate(new SteamAuthToken(validOpenIdParams())))
                .isInstanceOf(InvalidSteamCredentialException.class);
    }

    // ── getSteamUserProfile URL ───────────────────────────────────────────────

    @Test
    void getSteamUserProfile_buildsCorrectUrl() {
        String url = steamAuthProvider.getSteamUserProfile(STEAM_ID);
        assertThat(url).contains("steamids=" + STEAM_ID);
        assertThat(url).contains("key=FAKE_API_KEY");
        assertThat(url).contains("GetPlayerSummaries");
    }
}





