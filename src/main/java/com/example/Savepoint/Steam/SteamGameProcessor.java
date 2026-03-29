package com.example.Savepoint.Steam;

import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.GamePlatformIdRepository;
import com.example.Savepoint.Game.Repositories.GameRepository;
import com.example.Savepoint.Game.Repositories.UserGameRepository;
import com.example.Savepoint.Game.Services.GamePersistenceHelper;
import com.example.Savepoint.Game.Services.IgdbService;
import com.example.Savepoint.User.UserProfile;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SteamGameProcessor {
    GamePlatformIdRepository gamePlatformIdRepository;
    GameRepository gameRepository;
    UserGameRepository userGameRepository;
    IgdbService igdbService;
    GamePersistenceHelper gamePersistenceHelper;

    public SteamGameProcessor(GamePlatformIdRepository gamePlatformIdRepository, GameRepository gameRepository,UserGameRepository userGameRepository ,IgdbService igdbService, GamePersistenceHelper gamePersistenceHelper) {
        this.gamePlatformIdRepository = gamePlatformIdRepository;
        this.gameRepository = gameRepository;
        this.igdbService = igdbService;
        this.userGameRepository = userGameRepository;
        this.gamePersistenceHelper = gamePersistenceHelper;
    }

    // inject all repos and services

    @Transactional
    public void processGame(UserProfile user, SteamOwnedGame steamGame) {
        System.out.println(">>> processGame called for: " + steamGame.name());
        String steamAppId = steamGame.appid();

        // Step 1: Check GamePlatformId
        var result= gamePlatformIdRepository.findBySteamAppId(steamAppId);
        System.out.println(">>> platformId lookup result: " + result.isPresent());
        result.ifPresentOrElse(
                platformId -> {
                    // Game already mapped — just create UserGame
                    createUserGameIfAbsent(user, platformId.getGame(), steamGame);
                },
                () -> {
                    // Not found — IGDB two-step search by name
                    List<IgdbGame> searchResults = igdbService.searchByName(steamGame.name());
                    if (searchResults.isEmpty()) return;

                    IgdbGame igdbGame = searchResults.stream()
                            .filter(g -> g.name().equalsIgnoreCase(steamGame.name()))
                            .findFirst()
                            .orElse(searchResults.get(0));

                    searchResults.forEach(g -> System.out.println(">>> IGDB result: " + g.name()));

                    // Fetch platform data for this game
                    List<IgdbGame.Platform> platforms = igdbService.fetchByIds(List.of(igdbGame.id()))
                            .stream()
                            .findFirst()
                            .map(g -> g.platforms() == null ? List.<IgdbGame.Platform>of() : g.platforms())
                            .orElse(List.of());

                    // Check if Game already exists by igdbId (e.g. pre-seeded)
                    Game game = gameRepository.findByIgdbId(igdbGame.id())
                            .orElseGet(() -> gamePersistenceHelper.tryInsertGame(igdbGame, platforms));

                    // Insert GamePlatformId mapping
                    GamePlatformId mapping = GamePlatformId.builder()
                            .game(game)
                            .steamAppId(steamAppId)
                            .build();
                    gamePlatformIdRepository.save(mapping);

                    // Create UserGame
                    createUserGameIfAbsent(user, game, steamGame);
                }
        );
    }

    private void createUserGameIfAbsent(UserProfile user, Game game, SteamOwnedGame steamGame) {
        System.out.println(">>> createUserGameIfAbsent called, exists check: " +
                userGameRepository.existsByUser_IdAndGame_Id(user.getId(), game.getId()));
        boolean exists = userGameRepository.existsByUser_IdAndGame_Id(user.getId(), game.getId());
        if (!exists) {
            UserGame userGame = UserGame.builder()
                    .user(user)
                    .game(game)
                    .platform(UserGamePlatform.STEAM)
                    .hoursPlayed(steamGame.playtime_forever())
                    .lastPlayedAt(LocalDateTime.now())
                    .reviewStatus(ReviewStatus.UNREVIEWED)
                    .build();
            userGameRepository.save(userGame);
            System.out.println(">>> UserGame saved with id: " + userGame.getId());
        }
    }
}