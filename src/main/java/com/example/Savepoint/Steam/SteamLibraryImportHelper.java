package com.example.Savepoint.Steam;

import com.example.Savepoint.User.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class SteamLibraryImportHelper {
    private final UserProfileJpaRepositry userProfileJpaRepositry;
    private final SteamApiClient steamApiClient;
    private final SteamGameProcessor steamGameProcessor;
    @Qualifier("steamLibraryThreadPool")
    private final Executor steamLibraryThreadPool;

    @Async("lightTaskExecutor")
    public void importSteamLibrary(Integer userId, String steam64Id) {
        System.out.println(">>> importSteamLibrary called for user: " + userId);
        UserProfile user = userProfileJpaRepositry.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<SteamOwnedGame> ownedGames = steamApiClient.getOwnedGames(steam64Id);
        System.out.println(">>> ownedGames size: " + ownedGames.size());

//        for (SteamOwnedGame steamGame : ownedGames) {
//            try {
//                steamGameProcessor.processGame(user, steamGame);
//            } catch (Exception e) {
//                //System.out.println("Failed to process game " + steamGame.name() + ": " + e.getMessage());
//                e.printStackTrace();
//                // continue processing remaining games
//            }
//        }
        List<CompletableFuture<Void>> futures = ownedGames.stream()
                .map(steamGame -> CompletableFuture.runAsync(
                        () -> steamGameProcessor.processGame(user, steamGame),
                        steamLibraryThreadPool  // your named thread pool
                )
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
