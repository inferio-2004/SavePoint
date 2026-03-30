package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByIgdbId(Integer igdbId);
    Long countByIgdbId(Integer igdbId);
    @Query("SELECT DISTINCT g FROM Game g LEFT JOIN FETCH g.gameGenres gg LEFT JOIN FETCH gg.genre LEFT JOIN FETCH g.gamePlatforms LEFT JOIN FETCH g.gameDevelopers gd LEFT JOIN FETCH gd.developer")
    List<Game> findAllWithRelations();
}