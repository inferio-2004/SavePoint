package com.example.Savepoint.Review;

import com.example.Savepoint.Auth.AuthService;
import com.example.Savepoint.Auth.SessionAuthenticationToken;
import com.example.Savepoint.Review.DTO.ReviewDTO;
import com.example.Savepoint.Review.DTO.ReviewRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthService authService;

    // Save or update a draft. Idempotent — safe to call multiple times.
    @PostMapping("/{gameId}")
    public ResponseEntity<ReviewDTO> saveReview(
            @PathVariable Long gameId,
            @Valid @RequestBody ReviewRequestDTO dto,
            Authentication authentication) {
        Integer userId = authService.getCurrentUserId(authentication);
        return ResponseEntity.ok(reviewService.saveReview(gameId, dto, userId));
    }

    // Explicit publish action — separate from save to prevent accidental publishes.
    @PatchMapping("/{gameId}/publish")
    public ResponseEntity<ReviewDTO> publishReview(
            @PathVariable Long gameId,
            Authentication authentication) {
        Integer userId = authService.getCurrentUserId(authentication);
        return ResponseEntity.ok(reviewService.publishReview(gameId, userId));
    }

    // Public listing of published reviews for a game. Paginated.
    @GetMapping("/game/{gameId}")
    public ResponseEntity<Page<ReviewDTO>> getPublishedReviews(
            @PathVariable Long gameId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(reviewService.getPublishedReviews(gameId, pageable));
    }

    // Authenticated user's own review — includes draft state
    @GetMapping("/me/{gameId}")
    public ResponseEntity<ReviewDTO> getOwnReview(
            @PathVariable Long gameId,
            Authentication authentication) {
        Integer userId = authService.getCurrentUserId(authentication);
        return ResponseEntity.ok(reviewService.getOwnReview(gameId, userId));
    }

    // Adapt this to however your UserDetails stores the userId.
    // If you stored userId in a custom UserDetails, cast and call getUserId().
    // If not, inject UserService and look up by email: userDetails.getUsername()
}
