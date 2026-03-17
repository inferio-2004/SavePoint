package com.example.Savepoint.Game.Integration;

import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.Repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class GameRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private GameGenreRepository gameGenreRepository;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private GameDeveloperRepository gameDeveloperRepository;

    @Autowired
    private GamePlatformRepository gamePlatformRepository;

    private Game buildAndSaveGame(Integer igdbId, String title) {
        Game game = Game.builder()
                .igdbId(igdbId)
                .title(title)
                .build();
        return entityManager.persistAndFlush(game);
    }

    @Test
    void saveGame_persistsCorrectly() {
        Game game = buildAndSaveGame(119133, "Elden Ring");

        Optional<Game> found = gameRepository.findByIgdbId(119133);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Elden Ring");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void saveGame_duplicateIgdbId_throwsException() {
        buildAndSaveGame(119133, "Elden Ring");
        entityManager.clear();

        Game duplicate = Game.builder()
                .igdbId(119133)
                .title("Elden Ring Duplicate")
                .build();

        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }

    @Test
    void genre_findOrCreate_reusesExistingRow() {
        Genre first = genreRepository.save(Genre.builder().name("RPG").build());
        entityManager.flush();
        entityManager.clear();

        Genre second = genreRepository.findByName("RPG")
                .orElseGet(() -> genreRepository.save(Genre.builder().name("RPG").build()));

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(genreRepository.findAll()).hasSize(1);
    }

    @Test
    void genre_uniqueConstraint_preventsduplicates() {
        genreRepository.save(Genre.builder().name("RPG").build());
        entityManager.flush();

        assertThrows(Exception.class, () -> {
            genreRepository.save(Genre.builder().name("RPG").build());
            entityManager.flush();
        });
    }

    @Test
    void gameGenre_savesJoinRowCorrectly() {
        Game game = buildAndSaveGame(119133, "Elden Ring");
        Genre genre = entityManager.persistAndFlush(Genre.builder().name("RPG").build());

        GameGenre gameGenre = GameGenre.builder().game(game).genre(genre).build();
        gameGenreRepository.save(gameGenre);
        entityManager.flush();
        entityManager.clear();

        Game found = gameRepository.findByIgdbId(119133).get();
        assertThat(found.getGameGenres()).hasSize(1);
        assertThat(found.getGameGenres().iterator().next().getGenre().getName()).isEqualTo("RPG");
    }

    @Test
    void gamePlatform_savesCorrectly() {
        Game game = buildAndSaveGame(119133, "Elden Ring");

        GamePlatform platform = GamePlatform.builder()
                .game(game)
                .platformName("PC (Microsoft Windows)")
                .build();
        gamePlatformRepository.save(platform);
        entityManager.flush();
        entityManager.clear();

        Game found = gameRepository.findByIgdbId(119133).get();
        assertThat(found.getGamePlatforms()).hasSize(1);
        assertThat(found.getGamePlatforms().iterator().next().getPlatformName())
                .isEqualTo("PC (Microsoft Windows)");
    }

    @Test
    void developer_findOrCreate_reusesExistingRow() {
        Developer first = developerRepository.save(Developer.builder().name("FromSoftware").build());
        entityManager.flush();
        entityManager.clear();

        Developer second = developerRepository.findByName("FromSoftware")
                .orElseGet(() -> developerRepository.save(Developer.builder().name("FromSoftware").build()));

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(developerRepository.findAll()).hasSize(1);
    }

    @Test
    void deleteGame_cascadestoGenresAndPlatforms() {
        Game game = buildAndSaveGame(119133, "Elden Ring");
        Genre genre = entityManager.persistAndFlush(Genre.builder().name("RPG").build());
        gameGenreRepository.save(GameGenre.builder().game(game).genre(genre).build());
        gamePlatformRepository.save(GamePlatform.builder().game(game).platformName("PC (Microsoft Windows)").build());
        entityManager.flush();

        entityManager.flush();
        entityManager.clear(); // clear session before delete

        gameRepository.deleteById(game.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(gameRepository.findByIgdbId(119133)).isEmpty();
        assertThat(gameGenreRepository.findAll()).isEmpty();
        assertThat(gamePlatformRepository.findAll()).isEmpty();
        assertThat(genreRepository.findAll()).hasSize(1); // Genre itself should NOT be deleted
    }
}