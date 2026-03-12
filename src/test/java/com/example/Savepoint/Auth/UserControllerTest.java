package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.UserAlreadyExistsException;
import com.example.Savepoint.User.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)  // use real security rules so /auth/** is permitted
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;

    // SecurityConfig depends on these — must be mocked so context loads
    @MockitoBean SteamAuthProvider steamAuthProvider;
    @MockitoBean UserDetailsService userDetailsService;

    private final UserProfileDTO mockUser = new UserProfileDTO(1, "TestUser", "http://avatar.url");

    // ── GET /auth/steam ───────────────────────────────────────────────────────

    @Test
    void steamLogin_redirectsToSteam() throws Exception {
        when(authService.getRedirectUrlSteam()).thenReturn("https://steamcommunity.com/openid/login?openid.ns=test");

        mockMvc.perform(get("/auth/steam"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://steamcommunity.com/openid/login?openid.ns=test"));
    }

    // ── GET /auth/steam/callback ──────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void steamCallback_returns200WithUserProfile() throws Exception {
        when(authService.handleSteamCallback(any(Map.class), any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(mockUser);

        mockMvc.perform(get("/auth/steam/callback")
                        .param("openid.claimed_id", "https://steamcommunity.com/openid/id/123456")
                        .param("openid.mode", "id_res"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("TestUser"));
    }

    @Test
    void steamCallback_returns401_onInvalidSteamCredentials() throws Exception {
        when(authService.handleSteamCallback(any(), any(), any()))
                .thenThrow(new com.example.Savepoint.Exceptions.InvalidSteamCredentialException("Steam verification failed") {});

        mockMvc.perform(get("/auth/steam/callback")
                        .param("openid.claimed_id", "bad"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /auth/manual/login ───────────────────────────────────────────────

    @Test
    void manualLogin_returns200WithUserProfile() throws Exception {
        UserLoginDTO dto = new UserLoginDTO("password123", "test@mail.com");
        when(authService.handleManualLogin(any(), any(), any())).thenReturn(mockUser);

        mockMvc.perform(post("/auth/manual/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("TestUser"));
    }

    @Test
    void manualLogin_returns400_onBlankPassword() throws Exception {
        String body = "{\"password\":\"\",\"mail\":\"test@mail.com\"}";

        mockMvc.perform(post("/auth/manual/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualLogin_returns400_onInvalidEmail() throws Exception {
        String body = "{\"password\":\"password123\",\"mail\":\"not-an-email\"}";

        mockMvc.perform(post("/auth/manual/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualLogin_returns401_onBadCredentials() throws Exception {
        UserLoginDTO dto = new UserLoginDTO("wrongpass", "test@mail.com");
        when(authService.handleManualLogin(any(), any(), any()))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/manual/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /auth/manual/signup ──────────────────────────────────────────────

    @Test
    void manualSignup_returns201WithUserProfile() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(authService.handleManualRegister(any(), any(), any())).thenReturn(mockUser);

        mockMvc.perform(post("/auth/manual/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("TestUser"));
    }

    @Test
    void manualSignup_returns400_onShortPassword() throws Exception {
        String body = "{\"username\":\"TestUser\",\"password\":\"short\",\"mail\":\"test@mail.com\"}";

        mockMvc.perform(post("/auth/manual/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualSignup_returns400_onBlankUsername() throws Exception {
        String body = "{\"username\":\"\",\"password\":\"password123\",\"mail\":\"test@mail.com\"}";

        mockMvc.perform(post("/auth/manual/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualSignup_returns409_onDuplicateEmail() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO("TestUser", "password123", "test@mail.com");
        when(authService.handleManualRegister(any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("User with this email already exists"));

        mockMvc.perform(post("/auth/manual/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void manualSignup_returns400_onMissingFields() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/auth/manual/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
