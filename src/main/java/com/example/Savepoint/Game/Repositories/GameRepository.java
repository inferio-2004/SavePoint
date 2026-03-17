package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByIgdbId(Integer igdbId);
}