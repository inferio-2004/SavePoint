//package com.example.Savepoint.Game;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//import tools.jackson.databind.ObjectMapper;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//record AccessTokenResponse(String access_token, String token_type, int expires_in) {}
//
//record Cover(Integer id, String[] urls) {}
//record RawCover(Integer id, String url) {}
//
//record Genre(Integer id, String name) {}
//
//record Company(Integer id, String name) {}
//record InvolvedCompany(Integer id, Company company, boolean developer) {}
//record Platform(Integer id, String name) {}
//
//record Game(
//        Integer id,
//        Cover cover,
//        List<Genre> genres,
//        String name,
//        String summary,
//        Long first_release_date,
//        List<InvolvedCompany> involved_companies,
//        List<String> platforms
//) {}
//record RawGame(
//        Integer id,
//        RawCover cover,
//        List<Genre> genres,
//        String name,
//        String summary,
//        Long first_release_date,
//        List<InvolvedCompany> involved_companies,
//        List<Platform> platforms
//) {}
//
//@RestController
//public class GameController {
//    @Value("${igdb.client-id}")
//    String ClientID;
//    @Value("${igdb.client-secret}")
//    String ClientSecret;
//    RestTemplate restTemplate = new RestTemplate();
//
//    @GetMapping(path = "/test/igdb")
//    public Game[] searchGame(@RequestParam String gameName) {
//        String url = "https://id.twitch.tv/oauth2/token"
//                + "?client_id=%s"
//                + "&client_secret=%s"
//                + "&grant_type=client_credentials";
//        String formattedUrl = String.format(url, ClientID, ClientSecret);
//        AccessTokenResponse accessTokenResponse = restTemplate.postForObject(formattedUrl, null, AccessTokenResponse.class);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Client-ID", ClientID);
//        headers.set("Authorization", "Bearer " + accessTokenResponse.access_token());
//        headers.setContentType(MediaType.TEXT_PLAIN);
//
//        String body = String.format("fields name," +
//                "summary," +
//                "cover.url," +
//                "genres.name," +
//                "involved_companies.company.name, involved_companies.developer," +
//                "first_release_date;" +
//                "search \"%s\";" +
//                "where version_parent = null;" +
//                "limit 5;", gameName);
//
//        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
//
//        RawGame[] response = restTemplate.exchange(
//                "https://api.igdb.com/v4/games",
//                HttpMethod.POST,
//                httpEntity,
//                RawGame[].class
//        ).getBody();
//
//        // Step 2 — fetch full platform data by ID
//        String ids = Arrays.stream(response)
//                .map(g -> String.valueOf(g.id()))
//                .collect(Collectors.joining(", "));
//
//        String platformBody = "fields name, platforms.name; where id = (" + ids + ");";
//        HttpEntity<String> platformEntity = new HttpEntity<>(platformBody, headers);
//        RawGame[] fullData = restTemplate.exchange(
//                "https://api.igdb.com/v4/games",
//                HttpMethod.POST,
//                platformEntity,
//                RawGame[].class
//        ).getBody();
//
//        Map<Integer, List<Platform>> platformMap = Arrays.stream(fullData)
//                .collect(Collectors.toMap(RawGame::id, g -> g.platforms() == null ? List.of() : g.platforms()));
//
//        List<String> SUPPORTED_PLATFORMS = List.of(
//                "PC (Microsoft Windows)", "PlayStation 5", "PlayStation 4", "Nintendo Switch"
//        );
//
//        return Arrays.stream(response)
//                .map(game -> new Game(
//                        game.id(),
//                        new Cover(game.cover().id(), new String[]{
//                                "https:" + game.cover().url().replace("t_thumb", "t_thumb"),
//                                "https:" + game.cover().url().replace("t_thumb", "t_cover_big"),
//                                "https:" + game.cover().url().replace("t_thumb", "t_1080p")
//                        }),
//                        game.genres(),
//                        game.name(),
//                        game.summary(),
//                        game.first_release_date(),
//                        game.involved_companies(),
//                        platformMap.getOrDefault(game.id(), List.of()).stream()
//                                .map(Platform::name)
//                                .filter(SUPPORTED_PLATFORMS::contains)
//                                .toList()
//                ))
//                .toArray(Game[]::new);
//    }
//    @GetMapping(path="/test/igdb/testing_platforms")
//    public String searchGamePlatform() {
//        String url="https://id.twitch.tv/oauth2/token"
//                + "?client_id=%s"
//                + "&client_secret=%s"
//                + "&grant_type=client_credentials";
//        String formattedUrl=String.format(url, ClientID, ClientSecret);
//        AccessTokenResponse accessTokenResponse = restTemplate.postForObject(formattedUrl, null, AccessTokenResponse.class);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Client-ID", ClientID);
//        headers.set("Authorization", "Bearer " + accessTokenResponse.access_token());
//        headers.setContentType(MediaType.TEXT_PLAIN);
//        String body="fields name, platforms.name; where id = 112875;";//String.format("fields name, platforms.name; where id = (112875, 25076, ...);",gameName);
//        HttpEntity<String> httpEntity= new HttpEntity<>(
//                //String.format("fields name, genres.name, cover.url; search \"%s\"; limit 5;", gameName),
//                body,
//                headers
//        );
//
//        RawGame[] response = restTemplate.exchange(
//                "https://api.igdb.com/v4/games",
//                HttpMethod.POST,
//                httpEntity,
//                RawGame[].class
//        ).getBody();
//
//        String platformBody = String.format("fields *; where game = %d;", response[0].id());
//        HttpEntity<String> platformEntity = new HttpEntity<>(platformBody, headers);
//        return Arrays.stream(response)
//                .map(g -> g.name() + ": " + g.platforms())
//                .collect(Collectors.joining("\n"));
//    }
//}
