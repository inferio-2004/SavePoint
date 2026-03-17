package com.example.Savepoint.Game.Repositories;
import com.example.Savepoint.Game.Entities.Developer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DeveloperRepository extends JpaRepository<Developer, Integer> {
    Optional<Developer> findByName(String name);
}