package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.UserAlreadyExistsException;
import com.example.Savepoint.User.UserLoginDTO;
import com.example.Savepoint.User.UserProfileDTO;
import com.example.Savepoint.User.UserRegisterDTO;
import com.example.Savepoint.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String STEAM_ID = "76561198000000001";

    @Mock AuthenticationManager authenticationManager;
    @Mock UserService userService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RestTemplate restTemplate;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @InjectMocks AuthService authService;

    private final UserProfileDTO mockUser = new UserProfileDTO(1, "TestUser", "http://avatar.url");

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:5000");
        ReflectionTestUtils.setField(authService, "steamApiKey", "FAKE_API_KEY");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void getRedirectUrlSteam_containsReturnTo() {
        String url = authService.getRedirectUrlSteam();
        assertThat(url).contains("openid.return_to=http://localhost:5000/auth/steam/callback");
    }

    @Test
    void getRedirectUrlSteam_containsRealm() {
        String url = authService.getRedirectUrlSteam();
        assertThat(url).contains("openid.realm=http://localhost:5000");
    }

    @Test
    void getRedirectUrlSteam_pointsToSteamOpenId() {
        String url = authService.getRedirectUrlSteam();
        assertThat(url).startsWith("https://steamcommunity.com/openid/login");
    }

    @Test
    void handleSteamCallback_returnsUserProfileDTO_forExistingUser() {
        Map<String, String> params = Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID);
        SteamAuthToken authenticatedToken = new SteamAuthToken(params, STEAM_ID);
        when(authenticationManager.authenticate(any(SteamAuthToken.class))).thenReturn(authenticatedToken);
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.of(mockUser));

        UserProfileDTO result = authService.handleSteamCallback(params, request, response);

        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    void handleSteamCallback_createsSteamUser_whenNotFound() {
        Map<String, String> params = Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID);
        SteamAuthToken authenticatedToken = new SteamAuthToken(params, STEAM_ID);
        UserProfileDTO createdUser = new UserProfileDTO(2, "NewSteamUser", "http://new-avatar.url");
        SteamApiResponse apiResponse = new SteamApiResponse(
                new PlayersResponse(java.util.List.of(
                        new PlayerSummary(STEAM_ID, "NewSteamUser", "http://new-avatar.url")
                ))
        );

        when(authenticationManager.authenticate(any(SteamAuthToken.class))).thenReturn(authenticatedToken);
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(anyString(), eq(SteamApiResponse.class)))
                .thenReturn(ResponseEntity.ok(apiResponse));
        when(userService.createSteamUser(STEAM_ID, "NewSteamUser", "http://new-avatar.url"))
                .thenReturn(createdUser);

        UserProfileDTO result = authService.handleSteamCallback(params, request, response);

        assertThat(result).isEqualTo(createdUser);
        verify(userService).createSteamUser(STEAM_ID, "NewSteamUser", "http://new-avatar.url");
    }

    @Test
    void handleSteamCallback_delegatesToAuthManager() {
        Map<String, String> params = Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID);
        SteamAuthToken authenticatedToken = new SteamAuthToken(params, STEAM_ID);
        when(authenticationManager.authenticate(any(SteamAuthToken.class))).thenReturn(authenticatedToken);
        when(userService.findBySteamId(STEAM_ID)).thenReturn(Optional.of(mockUser));

        authService.handleSteamCallback(params, request, response);

        verify(authenticationManager).authenticate(any(SteamAuthToken.class));
    }

    @Test
    void handleSteamCallback_propagatesAuthException() {
        Map<String, String> params = Map.of("openid.claimed_id", "invalid");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("fail"));

        assertThatThrownBy(() -> authService.handleSteamCallback(params, request, response))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── handleManualLogin ─────────────────────────────────────────────────────

    @Test
    void handleManualLogin_returnsUserProfileDTO() {
        UserLoginDTO dto = new UserLoginDTO("password123", "test@mail.com");
        Authentication auth = new UsernamePasswordAuthenticationToken("test@mail.com", null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userService.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));

        UserProfileDTO result = authService.handleManualLogin(dto, request, response);

        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    void handleManualLogin_callsAuthManagerWithCorrectCredentials() {
        UserLoginDTO dto = new UserLoginDTO("password123", "test@mail.com");
        Authentication auth = new UsernamePasswordAuthenticationToken("test@mail.com", null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userService.findByEmail(any())).thenReturn(Optional.of(mockUser));

        authService.handleManualLogin(dto, request, response);

        verify(authenticationManager).authenticate(argThat(token ->
                token instanceof UsernamePasswordAuthenticationToken t &&
                "test@mail.com".equals(t.getPrincipal()) &&
                "password123".equals(t.getCredentials())
        ));
    }

    @Test
    void handleManualLogin_storesCustomSessionAuthentication() {
        UserLoginDTO dto = new UserLoginDTO("password123", "test@mail.com");
        Authentication auth = new UsernamePasswordAuthenticationToken("test@mail.com", null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userService.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));

        authService.handleManualLogin(dto, request, response);

        var session = ((MockHttpServletRequest) request).getSession(false);
        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext) session
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isInstanceOf(SessionAuthenticationToken.class);
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getPrincipal())
                .isEqualTo(new SessionAuthPrincipal(mockUser.id(), mockUser.displayName(), AuthProvider.MANUAL, null));
    }

    @Test
    void handleManualLogin_propagatesBadCredentials() {
        UserLoginDTO dto = new UserLoginDTO("wrongpass", "test@mail.com");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.handleManualLogin(dto, request, response))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── handleManualRegister ──────────────────────────────────────────────────

    @Test
    void handleManualRegister_returnsCreatedUser() {
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(userService.findByEmail("test@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userService.createManualUser(any())).thenReturn(mockUser);

        UserProfileDTO result = authService.handleManualRegister(dto, request, response);

        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    void handleManualRegister_throwsIfEmailAlreadyExists() {
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(userService.findByEmail("test@mail.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.handleManualRegister(dto, request, response))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void handleManualRegister_storesHashedPasswordNotPlainText() {
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(userService.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userService.createManualUser(any())).thenReturn(mockUser);

        authService.handleManualRegister(dto, request, response);

        // createManualUser must receive the HASHED password, never the plain one
        verify(userService).createManualUser(argThat(u -> u.password().equals("$2a$hashed")));
    }

    @Test
    void handleManualRegister_doesNotCallAuthManager() {
        // After register we build the session directly — no extra authenticate() call
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(userService.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userService.createManualUser(any())).thenReturn(mockUser);

        authService.handleManualRegister(dto, request, response);

        verify(authenticationManager, never()).authenticate(any());
    }
}

