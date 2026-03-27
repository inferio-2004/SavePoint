package com.example.Savepoint.Game.Integration;


import com.example.Savepoint.Game.Entities.ReviewStatus;
import com.example.Savepoint.Game.Entities.UserGame;
import com.example.Savepoint.Game.Entities.UserGamePlatform;
import com.example.Savepoint.Game.Repositories.GamePlatformIdRepository;
import com.example.Savepoint.Game.Repositories.UserGameRepository;
import com.example.Savepoint.Steam.SteamApiClient;
import com.example.Savepoint.Steam.SteamOwnedGame;
import com.example.Savepoint.Steam.SteamService;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class SteamOnboardingIntegrationTest {

    @Autowired
    private SteamService steamService;

    @Autowired
    private UserProfileJpaRepositry userProfileRepository;

    @Autowired
    private UserGameRepository userGameRepository;

    @Autowired
    private GamePlatformIdRepository gamePlatformIdRepository;

    @MockitoBean
    private SteamApiClient steamApiClient;

    private UserProfile testUser;

    @BeforeEach
    void setup() {
        // Create a test user directly in DB
        testUser = userProfileRepository.save(UserProfile.builder()
                .displayName("TestUser")
                .avatarUrl("http://avatar.test")
                .build());
    }

    @Test
    void testOnboarding_withPreSeededGame_createsUserGameAndPlatformId() throws InterruptedException {
        // Elden Ring — should already be in your DB from pre-seeding
        String steamAppId = "1245620";
        when(steamApiClient.getOwnedGames("76561198000000001"))
                .thenReturn(List.of(new SteamOwnedGame(steamAppId, "Elden Ring", 120)));

        steamService.importSteamLibrary(testUser.getId(), "76561198000000001");

        // @Async runs in background — wait for it
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
            assertThat(platformId).isPresent();
            assertThat(userGameRepository.existsByUser_IdAndGame_Id(
                    testUser.getId(), platformId.get().getGame().getId())).isTrue();
        });

        // GamePlatformId should be populated
        var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
        assertThat(platformId).isPresent();

        // UserGame should be created
        boolean userGameExists = userGameRepository.existsByUser_IdAndGame_Id(
                testUser.getId(), platformId.get().getGame().getId());
        assertThat(userGameExists).isTrue();

        // Verify platform and review status
        UserGame userGame = userGameRepository
                .findByUser_IdAndGame_Id(testUser.getId(), platformId.get().getGame().getId())
                .orElseThrow();
        assertThat(userGame.getPlatform()).isEqualTo(UserGamePlatform.STEAM);
        assertThat(userGame.getReviewStatus()).isEqualTo(ReviewStatus.UNREVIEWED);
        assertThat(userGame.getHoursPlayed()).isEqualTo(120);
    }

    @Test
    void testOnboarding_withUnseededGame_persistsGameAndCreatesUserGame() throws InterruptedException {
        // A niche game unlikely to be in your top 1000 seed
        String steamAppId = "999999999";
        when(steamApiClient.getOwnedGames("76561198000000001"))
                .thenReturn(List.of(new SteamOwnedGame(steamAppId, "Disco Elysium", 80)));

        steamService.importSteamLibrary(testUser.getId(), "76561198000000001");
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
            assertThat(platformId).isPresent();
            assertThat(userGameRepository.existsByUser_IdAndGame_Id(
                    testUser.getId(), platformId.get().getGame().getId())).isTrue();
        });

        var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
        assertThat(platformId).isPresent();

        boolean userGameExists = userGameRepository.existsByUser_IdAndGame_Id(
                testUser.getId(), platformId.get().getGame().getId());
        assertThat(userGameExists).isTrue();
    }

    @Test
    void testOnboarding_idempotent_doesNotDuplicateUserGame() throws InterruptedException {
        String steamAppId = "1245620";
        when(steamApiClient.getOwnedGames("76561198000000001"))
                .thenReturn(List.of(new SteamOwnedGame(steamAppId, "Elden Ring", 120)));

        // Run twice
        steamService.importSteamLibrary(testUser.getId(), "76561198000000001");
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
            assertThat(platformId).isPresent();
            assertThat(userGameRepository.existsByUser_IdAndGame_Id(
                    testUser.getId(), platformId.get().getGame().getId())).isTrue();
        });
        steamService.importSteamLibrary(testUser.getId(), "76561198000000001");
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var platformId = gamePlatformIdRepository.findBySteamAppId(steamAppId);
            assertThat(platformId).isPresent();
            assertThat(userGameRepository.existsByUser_IdAndGame_Id(
                    testUser.getId(), platformId.get().getGame().getId())).isTrue();
        });

        // Should still be only one UserGame row
        long count = userGameRepository.findAll().stream()
                .filter(ug -> ug.getUser().getId().equals(testUser.getId()))
                .count();
        assertThat(count).isEqualTo(1);
    }
    @AfterEach
    void cleanup() {
        userGameRepository.deleteAll();
        gamePlatformIdRepository.deleteAll();
        userProfileRepository.deleteById(testUser.getId());
    }
}