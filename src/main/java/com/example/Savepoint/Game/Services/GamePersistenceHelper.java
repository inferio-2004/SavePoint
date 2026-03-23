package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class GamePersistenceHelper {
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final GameGenreRepository gameGenreRepository;
    private final DeveloperRepository developerRepository;
    private final GameDeveloperRepository gameDeveloperRepository;
    private final GamePlatformRepository gamePlatformRepository;

    public GamePersistenceHelper(GameRepository gameRepository, GenreRepository genreRepository, GameGenreRepository gameGenreRepository, DeveloperRepository developerRepository, GameDeveloperRepository gameDeveloperRepository, GamePlatformRepository gamePlatformRepository) {
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.developerRepository = developerRepository;
        this.gameDeveloperRepository = gameDeveloperRepository;
        this.gamePlatformRepository = gamePlatformRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Game tryInsertGame(IgdbGame igdbGame, List<IgdbGame.Platform> platforms) {
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
}
