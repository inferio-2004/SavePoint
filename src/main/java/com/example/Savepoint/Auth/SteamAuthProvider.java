package com.example.Savepoint.Auth;

import com.example.Savepoint.Exceptions.InvalidSteamCredentialException;
import com.example.Savepoint.Exceptions.SteamUserNotFoundException;
import com.example.Savepoint.User.UserProfileDTO;
import com.example.Savepoint.User.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Component
public class SteamAuthProvider implements AuthenticationProvider {

    private final RestTemplate restTemplate;


    public SteamAuthProvider(RestTemplate restTemplate,UserService userService) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        SteamAuthToken token = (SteamAuthToken) auth;
        Map<String, String> params = (Map<String, String>) token.getCredentials();

        // Step 1 - Verify with Steam
        MultiValueMap<String, String> reqParams = new LinkedMultiValueMap<>();
        Map<String, String> verifyParams = new HashMap<>(params);
        verifyParams.put("openid.mode", "check_authentication");
        reqParams.setAll(verifyParams);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept-Encoding", "identity");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(reqParams, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://steamcommunity.com/openid/login", request, String.class);

        String isValid = response.getBody().split("\n")[1].split(":")[1].trim();
        if (!isValid.equals("true")) {
            throw new InvalidSteamCredentialException("Steam verification failed") {};
        }

        // Step 2 - Extract Steam64 ID
        String claimedId = params.get("openid.claimed_id");
        String steamId = claimedId.substring(claimedId.lastIndexOf("/") + 1);

        // Step 3 - Find or create user


        // Step 4 - Return authenticated token
        return new SteamAuthToken(params, steamId);
    }

    @Override
    public boolean supports(Class<?> auth) {
        return SteamAuthToken.class.isAssignableFrom(auth);
    }
}