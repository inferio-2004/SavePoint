package com.example.Savepoint.Game.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class IgdbTokenService {
    record AccessTokenResponse(String access_token, String token_type, int expires_in) {}
    private String token;
    private Instant expiresAt = Instant.EPOCH;
    @Value("${igdb.client-id}")
    private String ClientID;
    @Value("${igdb.client-secret}")
    private String ClientSecret;
    private RestTemplate restTemplate;

    public IgdbTokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private boolean checkToken(){
        return Instant.now().isAfter(expiresAt);
    }

    public String getToken() {
        if (checkToken()) {
            var response = restTemplate.postForObject(
                    "https://id.twitch.tv/oauth2/token?client_id=" + ClientID + "&client_secret=" + ClientSecret + "&grant_type=client_credentials",
                    null,
                    AccessTokenResponse.class
            );
            token = response.access_token();
            expiresAt = Instant.now().plusSeconds(response.expires_in()).minusSeconds(60); // refresh 1 minute before expiry
        }
        return token;
    }

}
