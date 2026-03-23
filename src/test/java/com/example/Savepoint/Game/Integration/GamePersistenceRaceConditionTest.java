package com.example.Savepoint.Game.Integration;

import com.example.Savepoint.Game.IgdbGame;
import com.example.Savepoint.Game.Repositories.GameRepository;
import com.example.Savepoint.Game.Services.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GamePersistenceRaceConditionTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void concurrentPersist_sameGame_onlyOneRowInserted() throws InterruptedException {
        IgdbGame igdbGame = new IgdbGame(
                119133,
                new IgdbGame.RawCover("//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg"),
                List.of(new IgdbGame.Genre("RPG")),
                "Elden Ring",
                "An action RPG",
                1645747200L,
                List.of(new IgdbGame.InvolvedCompany(new IgdbGame.Company("FromSoftware"), true)),
                null
        );
        List<IgdbGame.Platform> platforms = List.of(new IgdbGame.Platform("PC (Microsoft Windows)"));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // all threads wait here, then fire simultaneously
                    gameService.persistGame(igdbGame, platforms);
                } catch (Exception ignored) {}
            });
        }

        latch.countDown(); // release all threads at once
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long count = gameRepository.countByIgdbId(119133);
        assertThat(count).isEqualTo(1);
    }
}