package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.UserAlreadyExistsException;
import com.example.Savepoint.User.UserLoginDTO;
import com.example.Savepoint.User.UserProfileDTO;
import com.example.Savepoint.User.UserRegisterDTO;
import com.example.Savepoint.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository= new HttpSessionSecurityContextRepository();
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.base.url}")
    private String appBaseUrl;
    public AuthService(AuthenticationManager authenticationManager, UserService userService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public String getRedirectUrlSteam() {
        String template = "https://steamcommunity.com/openid/login" +
                "?openid.ns=http://specs.openid.net/auth/2.0" +
                "&openid.mode=checkid_setup" +
                "&openid.return_to=%s/auth/steam/callback" +
                "&openid.realm=%s" +
                "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select" +
                "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select";
        return String.format(template, appBaseUrl, appBaseUrl);
    }

    public UserProfileDTO handleSteamCallback(Map<String, String> authParams,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        SteamAuthToken authentication_token= new SteamAuthToken(authParams);
        var token=(SteamAuthToken)authenticationManager.authenticate(authentication_token);
        saveToSession(token, request, response);
        return (UserProfileDTO) token.getPrincipal();
    }

    public UserProfileDTO handleManualLogin(UserLoginDTO userLoginDTO,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        var unauthenticated = new UsernamePasswordAuthenticationToken(
                userLoginDTO.mail(), userLoginDTO.password());
        var authenticated = authenticationManager.authenticate(unauthenticated);
        saveToSession(authenticated, request, response);
        return userService.findByEmail(userLoginDTO.mail()).orElseThrow();
    }

    public UserProfileDTO handleManualRegister(UserRegisterDTO userRegisterDTO,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        if (userService.findByEmail(userRegisterDTO.mail()).isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        String hashedPassword = passwordEncoder.encode(userRegisterDTO.password());
        UserProfileDTO newUser = userService.createManualUser(userRegisterDTO.withPassword(hashedPassword));

        // No need to call authenticate() again — we just created the user, credentials are valid.
        // Build the authenticated token directly to avoid a redundant DB round-trip.
        var authenticated = new UsernamePasswordAuthenticationToken(
                userRegisterDTO.mail(), null, Collections.emptyList());
        saveToSession(authenticated, request, response);
        return newUser;
    }

    private void saveToSession(Authentication authenticated,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

}
