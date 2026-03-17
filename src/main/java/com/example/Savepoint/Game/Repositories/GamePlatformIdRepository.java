package com.example.Savepoint.Game.Repositories;

import com.example.Savepoint.Game.Entities.GamePlatformId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GamePlatformIdRepository extends JpaRepository<GamePlatformId, Integer> {
    Optional<GamePlatformId> findBySteamAppId(String steamAppId);
}
