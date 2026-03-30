package com.example.Savepoint.Social.Follow;

import com.example.Savepoint.Auth.AuthService;
import com.example.Savepoint.Auth.SessionAuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final AuthService authService;

    @PostMapping("/{followeeId}")
    public ResponseEntity<Void> follow(
            @PathVariable Integer followeeId,
            Authentication authentication) {
        Integer followerId = authService.getCurrentUserId(authentication);
        followService.follow(followerId, followeeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable Integer followeeId,
            Authentication authentication) {
        Integer followerId = authService.getCurrentUserId(authentication);
        followService.unfollow(followerId, followeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<FollowDTO>> getFollowers(
            @PathVariable Integer userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowers(userId, pageable));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<FollowDTO>> getFollowing(
            @PathVariable Integer userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.getFollowing(userId, pageable));
    }

}
