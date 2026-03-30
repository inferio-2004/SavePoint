package com.example.Savepoint.Social.Like;

import com.example.Savepoint.Auth.AuthService;
import com.example.Savepoint.Auth.SessionAuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final AuthService authService;

    @PostMapping("/{reviewId}")
    public ResponseEntity<Void> likeReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Integer userId = authService.getCurrentUserId(authentication);
        likeService.likeReview(userId, reviewId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> unlikeReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Integer userId = authService.getCurrentUserId(authentication);
        likeService.unlikeReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{reviewId}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long reviewId) {
        return ResponseEntity.ok(likeService.getLikeCount(reviewId));
    }

}
