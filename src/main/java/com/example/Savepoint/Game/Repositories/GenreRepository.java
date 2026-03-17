package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    Optional<Genre> findByName(String name);
}