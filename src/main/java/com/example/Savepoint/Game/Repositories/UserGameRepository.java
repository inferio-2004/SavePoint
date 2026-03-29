package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.UserGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    Optional<UserGame> findByUser_IdAndGame_Id(Integer userId, Long gameId);
    boolean existsByUser_IdAndGame_Id(Integer userId, Long gameId);
    @Query("SELECT ug FROM UserGame ug JOIN FETCH ug.game WHERE ug.user.id = :userId")
    Page<UserGame> findByUser_Id(@Param("userId") Integer userId, Pageable pageable);
}