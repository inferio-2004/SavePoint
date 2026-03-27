package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Game.DTO.GameDTO;
import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final GameGenreRepository gameGenreRepository;
    private final DeveloperRepository developerRepository;
    private final GameDeveloperRepository gameDeveloperRepository;
    private final GamePlatformRepository gamePlatformRepository;
    private final IgdbService igdbService;
    private final GamePersistenceHelper gamePersistenceHelper;

    public GameService(GameRepository gameRepository,
                       GenreRepository genreRepository,
                       GameGenreRepository gameGenreRepository,
                       DeveloperRepository developerRepository,
                       GameDeveloperRepository gameDeveloperRepository,
                       GamePlatformRepository gamePlatformRepository,
                       IgdbService igdbService, GamePersistenceHelper gamePersistenceHelper) {
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.developerRepository = developerRepository;
        this.gameDeveloperRepository = gameDeveloperRepository;
        this.gamePlatformRepository = gamePlatformRepository;
        this.igdbService = igdbService;
        this.gamePersistenceHelper = gamePersistenceHelper;
    }

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

    @Transactional
    public List<GameDTO> searchByName(String name) {
        List<IgdbGame> results = igdbService.searchByName(name);

        List<Integer> ids = results.stream()
                .map(IgdbGame::id)
                .toList();

        Map<Integer, List<IgdbGame.Platform>> platformMap = igdbService.fetchByIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        IgdbGame::id,
                        g -> g.platforms() == null ? List.of() : g.platforms()
                ));

        return results.stream()
                .map(igdbGame -> {
                    List<IgdbGame.Platform> platforms = platformMap.getOrDefault(igdbGame.id(), List.of());
                    Game game = persistGame(igdbGame, platforms);
                    return toDTO(game);
                })
                .toList();
    }

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