package com.example.Savepoint.Admin;

import com.example.Savepoint.Game.Services.GameService;
import com.example.Savepoint.Search.ElasticSearchIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ElasticSearchIndexService elasticsearchIndexService;
    private final GameService gameService;
    @PostMapping("/es/reindex")
    public ResponseEntity<String> reindex() {
        elasticsearchIndexService.reindexAll();
        return ResponseEntity.ok("Reindex complete");
    }

    @PostMapping("/games/seed")
    public ResponseEntity<String> preSeedGames(){
        gameService.seedTopGames();
        return ResponseEntity.ok().build();
    }
}