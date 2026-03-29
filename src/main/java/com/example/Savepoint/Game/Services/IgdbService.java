package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Game.Entities.Game;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IgdbService {

    @Value("${igdb.client-id}")
    private String clientID;
    private IgdbTokenService igdbTokenService;
    private RestTemplate restTemplate;

    public IgdbService(IgdbTokenService igdbTokenService,RestTemplate restTemplate,GameRepository gameRepository) {
        this.igdbTokenService = igdbTokenService;
        this.restTemplate = restTemplate;

    }

    public List<IgdbGame> searchByName(String gameName) {
        HttpHeaders headers = buildHeaders();

        String body = String.format("fields name," +
                "summary," +
                "cover.url," +
                "genres.name," +
                "involved_companies.company.name, involved_companies.developer," +
                "first_release_date;" +
                "search \"%s\";" +
                "where version_parent = null;" +
                "limit 5;", gameName);

        HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

        IgdbGame[] response = restTemplate.exchange(
                "https://api.igdb.com/v4/games",
                HttpMethod.POST,
                httpEntity,
                IgdbGame[].class
        ).getBody();
        return response == null ? List.of() : Arrays.asList(response);
    }

    public List<IgdbGame> fetchByIds(List<Integer> igdbIds) {
        String ids = igdbIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        HttpHeaders headers = buildHeaders();
        String body = "fields name, platforms.name; where id = (" + ids + ");";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        IgdbGame[] response = restTemplate.exchange(
                "https://api.igdb.com/v4/games",
                HttpMethod.POST,
                entity,
                IgdbGame[].class
        ).getBody();

        return response == null ? List.of() : Arrays.asList(response);
    }

    public List<IgdbGame> fetchTopGames(int offset, int limit) {
        HttpHeaders headers = buildHeaders();
        String body = String.format(
                "fields name, summary, cover.url, genres.name, " +
                        "involved_companies.company.name, involved_companies.developer, " +
                        "first_release_date; " +
                        "sort rating_count desc; " +
                        "where version_parent = null; " +
                        "limit %d; " +
                        "offset %d;",
                limit, offset
        );
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        IgdbGame[] response = restTemplate.exchange(
                "https://api.igdb.com/v4/games",
                HttpMethod.POST,
                entity,
                IgdbGame[].class
        ).getBody();

        return response == null ? List.of() : Arrays.asList(response);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientID);
        headers.set("Authorization", "Bearer " + igdbTokenService.getToken());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return headers;
    }


}
