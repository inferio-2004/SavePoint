package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.GamePlatform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePlatformRepository extends JpaRepository<GamePlatform, Long> {

}