package com.example.Savepoint.Game.Services;
import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.Repositories.*;
import com.example.Savepoint.Game.DTO.GameDTO;
import com.example.Savepoint.Game.IgdbGame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private GameGenreRepository gameGenreRepository;
    @Mock private DeveloperRepository developerRepository;
    @Mock private GameDeveloperRepository gameDeveloperRepository;
    @Mock private GamePlatformRepository gamePlatformRepository;
    @Mock private IgdbService igdbService;
    @Mock private GamePersistenceHelper gamePersistenceHelper;
    @Mock private ElasticsearchOperations elasticsearchOperations;
    @InjectMocks
    private GameService gameService;

    private IgdbGame buildIgdbGame() {
        return new IgdbGame(
                119133,
                new IgdbGame.RawCover("//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg"),
                List.of(new IgdbGame.Genre("RPG")),
                "Elden Ring",
                "An action RPG",
                1645747200L,
                List.of(new IgdbGame.InvolvedCompany(new IgdbGame.Company("FromSoftware"), true)),
                null
        );
    }

    @Test
    void persistGame_newGame_savesAllEntities() {
        IgdbGame igdbGame = buildIgdbGame();
        List<IgdbGame.Platform> platforms = List.of(new IgdbGame.Platform("PC (Microsoft Windows)"));

        Game savedGame = Game.builder().id(1L).igdbId(119133).title("Elden Ring").build();
        Genre savedGenre = Genre.builder().id(1L).name("RPG").build();
        Developer savedDev = Developer.builder().id(1L).name("FromSoftware").build();
        IgdbGame platformData = new IgdbGame(
                119133, null, null, "Elden Ring", null, null, null,
                List.of(new IgdbGame.Platform("PC (Microsoft Windows)"))
        );
        when(gameRepository.findByIgdbId(119133)).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenReturn(savedGame);
        when(genreRepository.findByName("RPG")).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);
        when(developerRepository.findByName("FromSoftware")).thenReturn(Optional.empty());
        when(developerRepository.save(any(Developer.class))).thenReturn(savedDev);
        when(igdbService.searchByName("Elden Ring")).thenReturn(List.of(igdbGame));
        when(igdbService.fetchByIds(any())).thenReturn(List.of(platformData));
        gameService.searchByName("Elden Ring");

        verify(gameRepository, times(1)).save(any(Game.class));
        verify(genreRepository, times(1)).save(any(Genre.class));
        verify(gameGenreRepository, times(1)).save(any(GameGenre.class));
        verify(developerRepository, times(1)).save(any(Developer.class));
        verify(gameDeveloperRepository, times(1)).save(any(GameDeveloper.class));
        verify(gamePlatformRepository, times(1)).save(any(GamePlatform.class));
    }

    @Test
    void persistGame_existingGame_skipsAllSaves() {
        IgdbGame igdbGame = buildIgdbGame();
        Game existingGame = Game.builder().id(1L).igdbId(119133).title("Elden Ring").build();

        when(gameRepository.findByIgdbId(119133)).thenReturn(Optional.of(existingGame));
        when(igdbService.searchByName("Elden Ring")).thenReturn(List.of(igdbGame));
        when(igdbService.fetchByIds(any())).thenReturn(List.of());

        gameService.searchByName("Elden Ring");

        verify(gameRepository, never()).save(any(Game.class));
        verify(genreRepository, never()).save(any(Genre.class));
        verify(gamePlatformRepository, never()).save(any(GamePlatform.class));
    }

    @Test
    void searchByName_mergesPlatformsCorrectly() {
        IgdbGame igdbGame = buildIgdbGame();
        IgdbGame platformData = new IgdbGame(
                119133, null, null, "Elden Ring", null, null, null,
                List.of(new IgdbGame.Platform("PC (Microsoft Windows)"))
        );

        Game savedGame = Game.builder()
                .id(1L)
                .igdbId(119133)
                .title("Elden Ring")
                .coverThumb("https://images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg")
                .build();

        when(igdbService.searchByName("Elden Ring")).thenReturn(List.of(igdbGame));
        when(igdbService.fetchByIds(List.of(119133))).thenReturn(List.of(platformData));
        when(gameRepository.findByIgdbId(119133)).thenReturn(Optional.empty());
        when(gameRepository.save(any())).thenReturn(savedGame);
        when(genreRepository.findByName(any())).thenReturn(Optional.empty());
        when(genreRepository.save(any())).thenReturn(Genre.builder().name("RPG").build());
        when(developerRepository.findByName(any())).thenReturn(Optional.empty());
        when(developerRepository.save(any())).thenReturn(Developer.builder().name("FromSoftware").build());

        List<GameDTO> results = gameService.searchByName("Elden Ring");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Elden Ring");
        verify(gamePlatformRepository, times(1)).save(any(GamePlatform.class));
    }

    @Test
    void seedTopGames_callsIgdbTwiceAndPersists() {
        IgdbGame igdbGame = buildIgdbGame();
        IgdbGame platformData = new IgdbGame(
                119133, null, null, "Elden Ring", null, null, null,
                List.of(new IgdbGame.Platform("PC (Microsoft Windows)"))
        );

        Game savedGame = Game.builder().id(1L).igdbId(119133).title("Elden Ring").build();

        when(igdbService.fetchTopGames(0, 500)).thenReturn(List.of(igdbGame));
        when(igdbService.fetchTopGames(500, 500)).thenReturn(List.of());
        when(igdbService.fetchByIds(any())).thenReturn(List.of(platformData));
        when(gameRepository.findByIgdbId(119133)).thenReturn(Optional.empty());
        when(gameRepository.save(any())).thenReturn(savedGame);
        when(genreRepository.findByName(any())).thenReturn(Optional.empty());
        when(genreRepository.save(any())).thenReturn(Genre.builder().name("RPG").build());
        when(developerRepository.findByName(any())).thenReturn(Optional.empty());
        when(developerRepository.save(any())).thenReturn(Developer.builder().name("FromSoftware").build());

        gameService.seedTopGames();

        verify(igdbService, times(1)).fetchTopGames(0, 500);
        verify(igdbService, times(1)).fetchTopGames(500, 500);
        verify(igdbService, times(1)).fetchByIds(any());
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    @Test
    void persistGame_raceCondition_fallsBackToFetch() {
        IgdbGame igdbGame = buildIgdbGame();
        Game existingGame = Game.builder().id(1L).igdbId(119133).title("Elden Ring").build();

        when(igdbService.searchByName("Elden Ring")).thenReturn(List.of(igdbGame));
        when(igdbService.fetchByIds(any())).thenReturn(List.of());
        when(gamePersistenceHelper.tryInsertGame(any(), any()))
                .thenThrow(DataIntegrityViolationException.class);
        when(gameRepository.findByIgdbId(119133)).thenReturn(Optional.of(existingGame));

        gameService.searchByName("Elden Ring");

        verify(gamePersistenceHelper, times(1)).tryInsertGame(any(), any());
        verify(gameRepository, times(1)).findByIgdbId(119133);
    }
}