package com.example.Savepoint.Search;

import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.Repositories.GameRepository;
import com.example.Savepoint.Game.Search.GameDocument;
import com.example.Savepoint.Game.Search.GameSearchRepository;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.User.Search.UserDocument;
import com.example.Savepoint.User.Search.UserSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticSearchIndexServiceTest {

    @Mock private GameSearchRepository gameSearchRepository;
    @Mock private UserSearchRepository userSearchRepository;
    @Mock private GameRepository gameRepository;
    @Mock private UserProfileJpaRepositry userProfileRepository;

    @InjectMocks
    private ElasticSearchIndexService elasticsearchIndexService;

    // ── indexGame ────────────────────────────────────────────────────────────

    @Test
    void indexGame_savesDocumentWithCorrectFields() {
        Game game = Game.builder()
                .id(1L)
                .title("Elden Ring")
                .description("An action RPG")
                .coverThumb("https://cover.jpg")
                .build();

        List<String> genres = List.of("RPG", "Action");
        List<String> platforms = List.of("PC", "PS5");
        List<String> developers = List.of("FromSoftware");

        elasticsearchIndexService.indexGame(game, genres, platforms, developers);

        ArgumentCaptor<GameDocument> captor = ArgumentCaptor.forClass(GameDocument.class);
        verify(gameSearchRepository).save(captor.capture());

        GameDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("1");
        assertThat(saved.getTitle()).isEqualTo("Elden Ring");
        assertThat(saved.getGenres()).containsExactly("RPG", "Action");
        assertThat(saved.getPlatforms()).containsExactly("PC", "PS5");
        assertThat(saved.getDevelopers()).containsExactly("FromSoftware");
    }

    @Test
    void indexGame_nullGenresAndPlatforms_savesEmptyLists() {
        Game game = Game.builder().id(2L).title("Unknown Game").build();

        elasticsearchIndexService.indexGame(game, List.of(), List.of(), List.of());

        ArgumentCaptor<GameDocument> captor = ArgumentCaptor.forClass(GameDocument.class);
        verify(gameSearchRepository).save(captor.capture());

        assertThat(captor.getValue().getGenres()).isEmpty();
        assertThat(captor.getValue().getPlatforms()).isEmpty();
        assertThat(captor.getValue().getDevelopers()).isEmpty();
    }

    // ── indexUser ────────────────────────────────────────────────────────────

    @Test
    void indexUser_savesDocumentWithCorrectFields() {
        UserProfile user = UserProfile.builder()
                .id(42)
                .displayName("Anirudh")
                .avatarUrl("https://avatar.jpg")
                .build();

        elasticsearchIndexService.indexUser(user);

        ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
        verify(userSearchRepository).save(captor.capture());

        UserDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("42");
        assertThat(saved.getDisplayName()).isEqualTo("Anirudh");
        assertThat(saved.getAvatarUrl()).isEqualTo("https://avatar.jpg");
    }

    // ── reindexAll ───────────────────────────────────────────────────────────

    @Test
    void reindexAll_deletesExistingIndexesBeforeRebuild() {
        when(gameRepository.findAllWithRelations()).thenReturn(List.of());
        when(userProfileRepository.findAll()).thenReturn(List.of());

        elasticsearchIndexService.reindexAll();

        verify(gameSearchRepository).deleteAll();
        verify(userSearchRepository).deleteAll();
    }

    @Test
    void reindexAll_gamesIndexRebuiltFromPostgres() {
        Genre rpg = Genre.builder().id(1L).name("RPG").build();
        Developer fromSoft = Developer.builder().id(1L).name("FromSoftware").build();

        Game game = Game.builder()
                .id(1L)
                .title("Elden Ring")
                .description("An action RPG")
                .coverThumb("https://cover.jpg")
                .build();

        GameGenre gameGenre = GameGenre.builder().game(game).genre(rpg).build();
        GamePlatform platform = GamePlatform.builder().game(game).platformName("PC").build();
        GameDeveloper gameDeveloper = GameDeveloper.builder().game(game).developer(fromSoft).build();

        game.getGameGenres().add(gameGenre);
        game.getGamePlatforms().add(platform);
        game.getGameDevelopers().add(gameDeveloper);

        when(gameRepository.findAllWithRelations()).thenReturn(List.of(game));
        when(userProfileRepository.findAll()).thenReturn(List.of());

        elasticsearchIndexService.reindexAll();

        ArgumentCaptor<List<GameDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameSearchRepository).saveAll(captor.capture());

        List<GameDocument> savedDocs = captor.getValue();
        assertThat(savedDocs).hasSize(1);
        assertThat(savedDocs.get(0).getTitle()).isEqualTo("Elden Ring");
        assertThat(savedDocs.get(0).getGenres()).containsExactly("RPG");
        assertThat(savedDocs.get(0).getPlatforms()).containsExactly("PC");
        assertThat(savedDocs.get(0).getDevelopers()).containsExactly("FromSoftware");
    }

    @Test
    void reindexAll_usersIndexRebuiltFromPostgres() {
        UserProfile user = UserProfile.builder()
                .id(1)
                .displayName("Anirudh")
                .avatarUrl("https://avatar.jpg")
                .build();

        when(gameRepository.findAllWithRelations()).thenReturn(List.of());
        when(userProfileRepository.findAll()).thenReturn(List.of(user));

        elasticsearchIndexService.reindexAll();

        ArgumentCaptor<List<UserDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(userSearchRepository).saveAll(captor.capture());

        List<UserDocument> savedDocs = captor.getValue();
        assertThat(savedDocs).hasSize(1);
        assertThat(savedDocs.get(0).getId()).isEqualTo("1");
        assertThat(savedDocs.get(0).getDisplayName()).isEqualTo("Anirudh");
    }

    @Test
    void reindexAll_emptyDatabase_savesEmptyCollections() {
        when(gameRepository.findAllWithRelations()).thenReturn(List.of());
        when(userProfileRepository.findAll()).thenReturn(List.of());

        elasticsearchIndexService.reindexAll();

        verify(gameSearchRepository).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
        verify(userSearchRepository).saveAll(argThat(list -> ((List<?>) list).isEmpty()));
    }
}