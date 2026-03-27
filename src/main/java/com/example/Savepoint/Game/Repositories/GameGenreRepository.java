package com.example.Savepoint.Game.Repositories;

import com.example.Savepoint.Game.Entities.Game;
import com.example.Savepoint.Game.Entities.GameGenre;
import com.example.Savepoint.Game.Entities.GameGenreId;
import com.example.Savepoint.Game.Entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameGenreRepository extends JpaRepository<GameGenre, GameGenreId> {
    boolean existsByGameAndGenre(Game game, Genre genre);

}
