package com.example.Savepoint.Review;

import com.example.Savepoint.Exceptions.ReviewNotFoundException;
import com.example.Savepoint.Game.Entities.Game;
import com.example.Savepoint.Exceptions.GameNotFoundException;
import com.example.Savepoint.Game.Repositories.GameRepository;
import com.example.Savepoint.Game.Entities.UserGame;
import com.example.Savepoint.Game.Repositories.UserGameRepository;
import com.example.Savepoint.Game.Entities.UserGamePlatform;
import com.example.Savepoint.Game.Entities.ReviewStatus;
import com.example.Savepoint.Review.DTO.ReviewDTO;
import com.example.Savepoint.Review.DTO.ReviewRequestDTO;
import com.example.Savepoint.Social.Follow.FollowService;
import com.example.Savepoint.Social.Notification.NotificationRepository;
import com.example.Savepoint.Social.Notification.NotificationService;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.Exceptions.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserGameRepository userGameRepository;
    private final GameRepository gameRepository;
    private final UserProfileJpaRepositry userProfileRepository;
    private final NotificationService notificationService;
    private final FollowService followService;

    // Called for both create and update — upsert behaviour.
    // Saves as draft. isVerified is determined once at creation and never changed.
    @Transactional
    public ReviewDTO saveReview(Long gameId, ReviewRequestDTO dto, Integer userId) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found"));

        // Get or create UserGame. Manual platform if no Steam import exists.
        UserGame userGame = userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseGet(() -> userGameRepository.save(
                        UserGame.builder()
                                .user(user)
                                .game(game)
                                .platform(UserGamePlatform.MANUAL)
                                .reviewStatus(ReviewStatus.DRAFT)
                                .build()
                ));

        // If UserGame existed but user hadn't touched review yet — move to draft
        if (userGame.getReviewStatus() == ReviewStatus.UNREVIEWED) {
            userGame.setReviewStatus(ReviewStatus.DRAFT);
        }

        // isVerified is a snapshot at review creation — not recalculated on edits.
        // A STEAM UserGame means the user actually played the game via Steam.
        boolean isVerified = userGame.getPlatform() == UserGamePlatform.STEAM;

        Review review = reviewRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseGet(() -> Review.builder()
                        .user(user)
                        .game(game)
                        .isVerified(isVerified)
                        .isPublished(false)
                        .build()
                );

        review.setRating(dto.rating());
        review.setReviewText(dto.reviewText());

        return toDTO(reviewRepository.save(review));
    }

    // Separate publish action — sets both Review.isPublished and UserGame.reviewStatus.
    // Both must stay in sync, which is why this is a dedicated method, not a flag in the request body.
    @Transactional
    public ReviewDTO publishReview(Long gameId, Integer userId) {
        Review review = reviewRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseThrow(() -> new ReviewNotFoundException("Save a draft before publishing"));

        UserGame userGame = userGameRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseThrow(() -> new ReviewNotFoundException("UserGame record missing"));

        review.setPublished(true);
        userGame.setReviewStatus(ReviewStatus.PUBLISHED);
        notificationService.notifyFollowersOfNewReview(followService.getFollowers(userId),review.getId());
        return toDTO(reviewRepository.save(review));
    }

    // Public listing — only published reviews visible
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getPublishedReviews(Long gameId, Pageable pageable) {
        return reviewRepository.findByGame_IdAndIsPublishedTrue(gameId, pageable)
                .map(this::toDTO);
    }

    // Owner's own review — draft or published
    @Transactional(readOnly = true)
    public ReviewDTO getOwnReview(Long gameId, Integer userId) {
        return reviewRepository.findByUser_IdAndGame_Id(userId, gameId)
                .map(this::toDTO)
                .orElseThrow(() -> new ReviewNotFoundException("No review found for this game"));
    }

    private ReviewDTO toDTO(Review review) {
        return new ReviewDTO(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getDisplayName(),
                review.getGame().getId(),
                review.getGame().getTitle(),
                review.getRating(),
                review.getReviewText(),
                review.isVerified(),
                review.isPublished(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
