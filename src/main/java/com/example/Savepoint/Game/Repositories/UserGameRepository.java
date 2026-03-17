package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    Optional<UserGame> findByUser_IdAndGame_Id(Integer userId, Long gameId);
    boolean existsByUser_IdAndGame_Id(Integer userId, Long gameId);
}