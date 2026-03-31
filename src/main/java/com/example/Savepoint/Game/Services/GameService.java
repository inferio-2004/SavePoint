package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Game.DTO.GameDTO;
import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.*;
import com.example.Savepoint.Game.Search.GameDocument;
import com.example.Savepoint.Search.ElasticSearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final GameGenreRepository gameGenreRepository;
    private final DeveloperRepository developerRepository;
    private final GameDeveloperRepository gameDeveloperRepository;
    private final GamePlatformRepository gamePlatformRepository;
    private final IgdbService igdbService;
    private final GamePersistenceHelper gamePersistenceHelper;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public void seedTopGames() {
        List<IgdbGame> firstBatch = igdbService.fetchTopGames(0, 500);
        List<IgdbGame> secondBatch = igdbService.fetchTopGames(500, 500);

        List<IgdbGame> allGames = new java.util.ArrayList<>();
        allGames.addAll(firstBatch);
        allGames.addAll(secondBatch);

        List<Integer> ids = allGames.stream()
                .map(IgdbGame::id)
                .toList();

        Map<Integer, List<IgdbGame.Platform>> platformMap = igdbService.fetchByIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        IgdbGame::id,
                        g -> g.platforms() == null ? List.of() : g.platforms()
                ));

        for (IgdbGame igdbGame : allGames) {
            List<IgdbGame.Platform> platforms = platformMap.getOrDefault(igdbGame.id(), List.of());
            persistGame(igdbGame, platforms);
        }
    }

    @Transactional(readOnly = true)
    public List<GameDTO> searchByName(String name) {
        NativeQuery query= NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm-> mm
                                .fields("title^3","description")
                                .query(name)
                                .fuzziness("AUTO")
                        )
                )
                .withPageable(org.springframework.data.domain.PageRequest.of(0, 20))
                .build();

        return elasticsearchOperations.search(query, GameDocument.class)
                .stream()
                .map(hit->{
                    GameDocument doc=hit.getContent();
                    return new GameDTO(
                            Long.parseLong(doc.getId()),
                            doc.getTitle(),
                            doc.getCoverThumb(),
                            doc.getReleaseDate(),
                            doc.getGenres()!=null?doc.getGenres():List.of(),
                            doc.getDevelopers()!=null?doc.getDevelopers():List.of(),
                            doc.getPlatforms()!=null?doc.getPlatforms():List.of()
                    );
                }).toList();
    }

    @Cacheable(value = "games", key = "#id")
    public GameDTO getGameById(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        return toDTO(game);
    }


    public Game persistGame(IgdbGame igdbGame, List<IgdbGame.Platform> platforms) {
        return gamePersistenceHelper.tryInsertGame(igdbGame, platforms);
    }

    private GameDTO toDTO(Game game) {
        List<String> genres = game.getGameGenres().stream()
                .map(gg -> gg.getGenre().getName())
                .toList();

        List<String> developers = game.getGameDevelopers().stream()
                .map(gd -> gd.getDeveloper().getName())
                .toList();

        List<String> platforms = game.getGamePlatforms().stream()
                .map(GamePlatform::getPlatformName)
                .toList();

        return new GameDTO(
                game.getId(),
                game.getTitle(),
                game.getCoverThumb(),
                game.getReleaseDate(),
                genres,
                developers,
                platforms
        );
    }
}