package com.example.Savepoint.Game;

import com.example.Savepoint.Auth.AuthService;
import com.example.Savepoint.Auth.SessionAuthenticationToken;
import com.example.Savepoint.Game.DTO.UserGameDTO;
import com.example.Savepoint.Game.Services.UserGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserGameController {
    private final UserGameService userGameService;
    private final AuthService authService;

    @GetMapping("/api/gamelist")
    public ResponseEntity<Page<UserGameDTO>> getUserGamelist(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable,Authentication authentication) {
        Integer userId=authService.getCurrentUserId(authentication);
        return ResponseEntity.ok(userGameService.getUserGames(userId,pageable));
    }
}
