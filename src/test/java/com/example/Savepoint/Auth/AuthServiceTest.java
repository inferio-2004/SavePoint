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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserService userService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    @InjectMocks AuthService authService;

    private final UserProfileDTO mockUser = new UserProfileDTO(1, "TestUser", "http://avatar.url");

    @BeforeEach
    void setup() {
        // inject @Value field
        ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:5000");
    }

    // ── getRedirectUrlSteam ───────────────────────────────────────────────────

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

    // ── handleSteamCallback ───────────────────────────────────────────────────

    @Test
    void handleSteamCallback_returnsUserProfileDTO() {
        Map<String, String> params = Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/123456");
        SteamAuthToken authenticatedToken = new SteamAuthToken(params, mockUser);
        when(authenticationManager.authenticate(any(SteamAuthToken.class))).thenReturn(authenticatedToken);

        UserProfileDTO result = authService.handleSteamCallback(params, request, response);

        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    void handleSteamCallback_delegatesToAuthManager() {
        Map<String, String> params = Map.of("openid.claimed_id", "https://steamcommunity.com/openid/id/123456");
        SteamAuthToken authenticatedToken = new SteamAuthToken(params, mockUser);
        when(authenticationManager.authenticate(any(SteamAuthToken.class))).thenReturn(authenticatedToken);

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
                t.getPrincipal().equals("test@mail.com") &&
                t.getCredentials().equals("password123")
        ));
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

