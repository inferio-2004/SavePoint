package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.SteamUserNotFoundException;
import com.example.Savepoint.Exceptions.UserAlreadyExistsException;
import com.example.Savepoint.Steam.SteamService;
import com.example.Savepoint.User.UserLoginDTO;
import com.example.Savepoint.User.UserProfileDTO;
import com.example.Savepoint.User.UserRegisterDTO;
import com.example.Savepoint.User.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository= new HttpSessionSecurityContextRepository();
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final SteamService steamService;
    @Value("${app.base.url}")
    private String appBaseUrl;
    @Value("${steam.api.key}")
    private String steamApiKey;

    public AuthService(AuthenticationManager authenticationManager, UserService userService, PasswordEncoder passwordEncoder, RestTemplate restTemplate, SteamService steamService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.steamService = steamService;
    }

    public String getSteamUserProfile(String steamUserId) {
        String template="https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/" +
                "?key=%s" +
                "&steamids=%s";
        return String.format(template, steamApiKey, steamUserId);
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
        String steamId = (String) token.getPrincipal();
        UserProfileDTO user = userService.findBySteamId(steamId)
                .orElseGet(() -> {
                    ResponseEntity<SteamApiResponse> profileResponse =
                            restTemplate.getForEntity(getSteamUserProfile(steamId), SteamApiResponse.class);
                    try{
                        String displayName = profileResponse.getBody().response().players().get(0).personaname();
                        String avatarUrl = profileResponse.getBody().response().players().get(0).avatarfull();
                        return userService.createSteamUser(steamId, displayName, avatarUrl);
                    }catch(IndexOutOfBoundsException err){
                        throw new SteamUserNotFoundException("Steam user not found");
                    }
                });
        var principal = new SessionAuthPrincipal(user.id(), user.displayName(), AuthProvider.STEAM, steamId);
        var sessionAuthentication = new SessionAuthenticationToken(principal, token.getAuthorities());
        saveToSession(sessionAuthentication, request, response);
        steamService.importSteamLibrary(user.id(), steamId);
        return user;
    }

    public UserProfileDTO handleManualLogin(UserLoginDTO userLoginDTO,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        var unauthenticated = new UsernamePasswordAuthenticationToken(
                userLoginDTO.mail(), userLoginDTO.password());
        var authenticated = authenticationManager.authenticate(unauthenticated);
        var user=userService.findByEmail(userLoginDTO.mail()).orElseThrow();
        var principal = new SessionAuthPrincipal(user.id(), user.displayName(), AuthProvider.MANUAL, null);
        var sessionAuthentication = new SessionAuthenticationToken(principal, authenticated.getAuthorities());
        saveToSession(sessionAuthentication, request, response);
        return user;
    }

    public UserProfileDTO handleManualRegister(UserRegisterDTO userRegisterDTO,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        if (userService.findByEmail(userRegisterDTO.mail()).isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        String hashedPassword = passwordEncoder.encode(userRegisterDTO.password());
        UserProfileDTO newUser = userService.createManualUser(userRegisterDTO.withPassword(hashedPassword));

        var principal = new SessionAuthPrincipal(newUser.id(), newUser.displayName(), AuthProvider.MANUAL, null);
        var sessionAuthentication = new SessionAuthenticationToken(principal);
        saveToSession(sessionAuthentication, request, response);
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

    public Integer getCurrentUserId(Authentication authentication) {
        SessionAuthenticationToken principal = (SessionAuthenticationToken) authentication.getPrincipal();
        return principal.getPrincipal().id();
    }

}
