package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Game.DTO.GameDTO;
import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.*;
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

    public GameService(GameRepository gameRepository,
                       GenreRepository genreRepository,
                       GameGenreRepository gameGenreRepository,
                       DeveloperRepository developerRepository,
                       GameDeveloperRepository gameDeveloperRepository,
                       GamePlatformRepository gamePlatformRepository,
                       IgdbService igdbService) {
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.developerRepository = developerRepository;
        this.gameDeveloperRepository = gameDeveloperRepository;
        this.gamePlatformRepository = gamePlatformRepository;
        this.igdbService = igdbService;
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

    private Game persistGame(IgdbGame igdbGame, List<IgdbGame.Platform> platforms) {
        // Skip if already exists
        return gameRepository.findByIgdbId(igdbGame.id()).orElseGet(() -> {
            // Build cover URLs
            String thumbUrl = null, bigUrl = null, hdUrl = null;
            if (igdbGame.cover() != null) {
                String raw = "https:" + igdbGame.cover().url();
                thumbUrl = raw;
                bigUrl = raw.replace("t_thumb", "t_cover_big");
                hdUrl = raw.replace("t_thumb", "t_1080p");
            }

            // Build release date
            LocalDate releaseDate = igdbGame.first_release_date() == null ? null :
                    Instant.ofEpochSecond(igdbGame.first_release_date())
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();

            // Save Game
            Game game = gameRepository.save(Game.builder()
                    .igdbId(igdbGame.id())
                    .title(igdbGame.name())
                    .description(igdbGame.summary())
                    .coverThumb(thumbUrl)
                    .coverBig(bigUrl)
                    .cover1080p(hdUrl)
                    .releaseDate(releaseDate)
                    .build());

            // Save Genres
            if (igdbGame.genres() != null) {
                for (IgdbGame.Genre g : igdbGame.genres()) {
                    Genre genre = genreRepository.findByName(g.name())
                            .orElseGet(() -> genreRepository.save(Genre.builder().name(g.name()).build()));
                    gameGenreRepository.save(GameGenre.builder().game(game).genre(genre).build());
                }
            }

            // Save Developers
            if (igdbGame.involved_companies() != null) {
                for (IgdbGame.InvolvedCompany ic : igdbGame.involved_companies()) {
                    if (ic.developer()) {
                        Developer dev = developerRepository.findByName(ic.company().name())
                                .orElseGet(() -> developerRepository.save(Developer.builder().name(ic.company().name()).build()));
                        gameDeveloperRepository.save(GameDeveloper.builder().game(game).developer(dev).build());
                    }
                }
            }

            // Save Platforms
            for (IgdbGame.Platform p : platforms) {
                gamePlatformRepository.save(GamePlatform.builder().game(game).platformName(p.name()).build());
            }

            return game;
        });
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