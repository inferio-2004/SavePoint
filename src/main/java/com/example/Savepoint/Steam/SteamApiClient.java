package com.example.Savepoint.Steam;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

@Component
public class SteamApiClient {

    @Value("${steam.api.key}")
    private String steamApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SteamOwnedGame> getOwnedGames(String steam64Id) {
        String url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/" +
                "?key=" + steamApiKey +
                "&steamid=" + steam64Id +
                "&include_appinfo=true" +
                "&include_played_free_games=true";

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode games = objectMapper.readTree(response)
                    .path("response")
                    .path("games");

            List<SteamOwnedGame> result = new ArrayList<>();
            if (games.isArray()) {
                for (JsonNode node : games) {
                    result.add(new SteamOwnedGame(
                            node.path("appid").asText(),
                            node.path("name").asText(),
                            node.path("playtime_forever").asInt()
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("Failed to fetch Steam library for " + steam64Id + ": " + e.getMessage());
            return List.of();
        }
    }
}