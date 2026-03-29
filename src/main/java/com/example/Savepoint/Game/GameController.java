package com.example.Savepoint.Game;


import com.example.Savepoint.Exceptions.GameNotFoundException;
import com.example.Savepoint.Game.DTO.GameDTO;
import com.example.Savepoint.Game.Services.GameService;
import com.example.Savepoint.Game.Services.IgdbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GameController {
    GameService gameService;
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }


    @PostMapping("/api/games/seed")
    public ResponseEntity<String> preSeedGames(){
        gameService.seedTopGames();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/games/search")
    public ResponseEntity<List<GameDTO>> searchGames(@RequestParam String gameName) throws GameNotFoundException {
        List<GameDTO> games = gameService.searchByName(gameName);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/api/games/{id}")
    public ResponseEntity<GameDTO> getGame(@PathVariable Long id) throws GameNotFoundException {
        GameDTO game = gameService.getGameById(id);
        if (game != null) {
            return ResponseEntity.ok(game);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
