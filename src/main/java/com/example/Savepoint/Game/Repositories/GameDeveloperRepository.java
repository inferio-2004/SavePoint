package com.example.Savepoint.Game.Repositories;

import com.example.Savepoint.Game.Entities.Developer;
import com.example.Savepoint.Game.Entities.Game;
import com.example.Savepoint.Game.Entities.GameDeveloper;
import com.example.Savepoint.Game.Entities.GameDeveloperId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameDeveloperRepository extends JpaRepository<GameDeveloper, GameDeveloperId> {
    boolean existsByGameAndDeveloper(Game game, Developer developer);

}
