package com.example.Savepoint.Search;

import com.example.Savepoint.Game.Entities.Game;
import com.example.Savepoint.Game.Entities.GamePlatform;
import com.example.Savepoint.Game.Repositories.GameRepository;
import com.example.Savepoint.Game.Search.GameDocument;
import com.example.Savepoint.Game.Search.GameSearchRepository;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.User.Search.UserDocument;
import com.example.Savepoint.User.Search.UserSearchRepository;
import com.example.Savepoint.User.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ElasticSearchIndexService {

    private final GameSearchRepository gameSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final GameRepository gameRepository;
    private final UserProfileJpaRepositry userProfileRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // Called by GamePersistenceHelper after a new game is saved to Postgres
    public void indexGame(Game game, List<String> genres, List<String> platforms, List<String> developers) {
        gameSearchRepository.save(GameDocument.builder()
                .id(game.getId().toString())
                .title(game.getTitle())
                .description(game.getDescription())
                .genres(genres)
                .platforms(platforms)
                .coverThumb(game.getCoverThumb())
                .releaseDate(game.getReleaseDate())
                .developers(developers)
                .build());
    }

    // Called by UserService after a new user is created
    public void indexUser(UserProfile user) {
        userSearchRepository.save(UserDocument.builder()
                .id(user.getId().toString())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }

    // Wipes both indexes and rebuilds from Postgres — run once after deploy,
    // or anytime the index gets out of sync
    public void reindexAll() {
        reindexGames();
        reindexUsers();
    }

    private void reindexGames() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(GameDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<GameDocument> docs = gameRepository.findAllWithRelations().stream()
                .map(game -> {
                    List<String> genres = game.getGameGenres().stream()
                            .map(gg -> gg.getGenre().getName())
                            .toList();
                    List<String> platforms = game.getGamePlatforms().stream()
                            .map(GamePlatform::getPlatformName)
                            .toList();

                    List<String> developers = game.getGameDevelopers().stream()
                            .map(gd -> gd.getDeveloper().getName())
                            .toList();

                    return GameDocument.builder()
                            .id(game.getId().toString())
                            .title(game.getTitle())
                            .description(game.getDescription())
                            .genres(genres)
                            .developers(developers)
                            .platforms(platforms)
                            .coverThumb(game.getCoverThumb())
                            .releaseDate(game.getReleaseDate())
                            .build();
                }).toList();

        gameSearchRepository.saveAll(docs);
    }

    private void reindexUsers() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(UserDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<UserDocument> docs = userProfileRepository.findAll().stream()
                .filter(user-> user.getRole()!= UserRole.ADMIN)
                .map(user -> UserDocument.builder()
                        .id(user.getId().toString())
                        .displayName(user.getDisplayName())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .toList();

        userSearchRepository.saveAll(docs);
    }
}
