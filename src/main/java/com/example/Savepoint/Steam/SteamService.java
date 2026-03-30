package com.example.Savepoint.Steam;

import com.example.Savepoint.Auth.AuthProvider;
import com.example.Savepoint.Auth.UserAuth;
import com.example.Savepoint.Game.Entities.*;
import com.example.Savepoint.Game.Repositories.*;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.Auth.UserAuthJpaRepositry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SteamService {

    private final SteamApiClient steamApiClient;
    private final SteamGameProcessor steamGameProcessor;
    private final UserProfileJpaRepositry userProfileRepositry;
    private final UserAuthJpaRepositry userAuthJpaRepositry;

    public SteamService(SteamApiClient steamApiClient,
                        SteamGameProcessor steamGameProcessor, UserProfileJpaRepositry userProfileRepositry, UserAuthJpaRepositry userAuthJpaRepositry) {
        this.steamApiClient = steamApiClient;
        this.steamGameProcessor = steamGameProcessor;
        this.userProfileRepositry = userProfileRepositry;
        this.userAuthJpaRepositry = userAuthJpaRepositry;
    }

    @Async
    public void importSteamLibrary(Integer userId, String steam64Id) {
        System.out.println(">>> importSteamLibrary called for user: " + userId);
        UserProfile user = userProfileRepositry.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<SteamOwnedGame> ownedGames = steamApiClient.getOwnedGames(steam64Id);
        System.out.println(">>> ownedGames size: " + ownedGames.size());

        for (SteamOwnedGame steamGame : ownedGames) {
            try {
                steamGameProcessor.processGame(user, steamGame);
            } catch (Exception e) {
                //System.out.println("Failed to process game " + steamGame.name() + ": " + e.getMessage());
                e.printStackTrace();
                // continue processing remaining games
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // every 24 hours
    public void syncAllSteamLibraries() {
        List<UserAuth> steamUsers = userAuthJpaRepositry.findByProvider(AuthProvider.STEAM);
        for (UserAuth auth : steamUsers) {
            importSteamLibrary(auth.getUser().getId(), auth.getProviderUserId());;
        }
    }

}