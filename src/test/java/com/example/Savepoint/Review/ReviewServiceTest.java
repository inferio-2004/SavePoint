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
import com.example.Savepoint.Exceptions.BadCredentialsException;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock UserGameRepository userGameRepository;
    @Mock GameRepository gameRepository;
    @Mock UserProfileJpaRepositry userProfileRepository;

    @InjectMocks ReviewService reviewService;

    private UserProfile user;
    private Game game;
    private final Integer USER_ID = 1;
    private final Long GAME_ID = 10L;

    @BeforeEach
    void setUp() {
        user = UserProfile.builder().id(USER_ID).displayName("testuser").build();
        game = Game.builder().id(GAME_ID).title("Elden Ring").build();
    }

    // --- saveReview ---

    @Test
    void saveReview_noExistingUserGame_createsManualUserGameAndUnverifiedReview() {
        ReviewRequestDTO dto = new ReviewRequestDTO(8, "Great game");

        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.empty());

        UserGame savedUserGame = UserGame.builder()
                .user(user).game(game)
                .platform(UserGamePlatform.MANUAL)
                .reviewStatus(ReviewStatus.DRAFT)
                .build();
        when(userGameRepository.save(any(UserGame.class))).thenReturn(savedUserGame);
        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.empty());

        Review savedReview = Review.builder()
                .id(1L).user(user).game(game)
                .rating(8).reviewText("Great game")
                .isVerified(false).isPublished(false)
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewDTO result = reviewService.saveReview(GAME_ID, dto, USER_ID);

        assertThat(result.isVerified()).isFalse();
        assertThat(result.rating()).isEqualTo(8);
        assertThat(result.isPublished()).isFalse();

        // Verify UserGame was created with MANUAL platform
        verify(userGameRepository).save(argThat(ug ->
                ug.getPlatform() == UserGamePlatform.MANUAL &&
                ug.getReviewStatus() == ReviewStatus.DRAFT
        ));
    }

    @Test
    void saveReview_existingSteamUserGame_setsVerifiedTrue() {
        ReviewRequestDTO dto = new ReviewRequestDTO(9, "Masterpiece");

        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        UserGame steamUserGame = UserGame.builder()
                .user(user).game(game)
                .platform(UserGamePlatform.STEAM)
                .reviewStatus(ReviewStatus.UNREVIEWED)
                .build();
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(steamUserGame));
        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.empty());

        Review savedReview = Review.builder()
                .id(2L).user(user).game(game)
                .rating(9).reviewText("Masterpiece")
                .isVerified(true).isPublished(false)
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewDTO result = reviewService.saveReview(GAME_ID, dto, USER_ID);

        assertThat(result.isVerified()).isTrue();
        // UserGame status should have moved from UNREVIEWED → DRAFT
        assertThat(steamUserGame.getReviewStatus()).isEqualTo(ReviewStatus.DRAFT);
    }

    @Test
    void saveReview_existingDraft_updatesRatingAndText() {
        ReviewRequestDTO dto = new ReviewRequestDTO(7, "Updated thoughts");

        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        UserGame existingUserGame = UserGame.builder()
                .user(user).game(game)
                .platform(UserGamePlatform.MANUAL)
                .reviewStatus(ReviewStatus.DRAFT)
                .build();
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(existingUserGame));

        Review existingReview = Review.builder()
                .id(3L).user(user).game(game)
                .rating(5).reviewText("Old text")
                .isVerified(false).isPublished(false)
                .build();
        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDTO result = reviewService.saveReview(GAME_ID, dto, USER_ID);

        assertThat(result.rating()).isEqualTo(7);
        assertThat(result.reviewText()).isEqualTo("Updated thoughts");
        // UserGame save should NOT be called again — it already exists
        verify(userGameRepository, never()).save(any());
    }

    @Test
    void saveReview_userNotFound_throwsUserNotFoundException() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.saveReview(GAME_ID, new ReviewRequestDTO(5, null), USER_ID)
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void saveReview_gameNotFound_throwsGameNotFoundException() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.saveReview(GAME_ID, new ReviewRequestDTO(5, null), USER_ID)
        ).isInstanceOf(GameNotFoundException.class);
    }

    // --- publishReview ---

    @Test
    void publishReview_existingDraft_setsPublishedAndUpdatesUserGame() {
        Review draft = Review.builder()
                .id(4L).user(user).game(game)
                .rating(8).reviewText("Good game")
                .isVerified(false).isPublished(false)
                .build();
        UserGame userGame = UserGame.builder()
                .user(user).game(game)
                .platform(UserGamePlatform.MANUAL)
                .reviewStatus(ReviewStatus.DRAFT)
                .build();

        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(draft));
        when(userGameRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(userGame));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDTO result = reviewService.publishReview(GAME_ID, USER_ID);

        assertThat(result.isPublished()).isTrue();
        assertThat(userGame.getReviewStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    void publishReview_noExistingReview_throwsReviewNotFoundException() {
        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.publishReview(GAME_ID, USER_ID)
        ).isInstanceOf(ReviewNotFoundException.class);
    }

    // --- getPublishedReviews ---

    @Test
    void getPublishedReviews_returnsOnlyPublishedReviews() {
        Review published = Review.builder()
                .id(5L).user(user).game(game)
                .rating(9).reviewText("Amazing")
                .isVerified(true).isPublished(true)
                .build();

        PageRequest pageable = PageRequest.of(0, 20);
        when(reviewRepository.findByGame_IdAndIsPublishedTrue(GAME_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(published)));

        Page<ReviewDTO> result = reviewService.getPublishedReviews(GAME_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isPublished()).isTrue();
    }

    // --- getOwnReview ---

    @Test
    void getOwnReview_returnsDraftForOwner() {
        Review draft = Review.builder()
                .id(6L).user(user).game(game)
                .rating(6).reviewText("Still thinking")
                .isVerified(false).isPublished(false)
                .build();

        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.of(draft));

        ReviewDTO result = reviewService.getOwnReview(GAME_ID, USER_ID);

        assertThat(result.isPublished()).isFalse();
        assertThat(result.reviewText()).isEqualTo("Still thinking");
    }

    @Test
    void getOwnReview_noReview_throwsReviewNotFoundException() {
        when(reviewRepository.findByUser_IdAndGame_Id(USER_ID, GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reviewService.getOwnReview(GAME_ID, USER_ID)
        ).isInstanceOf(ReviewNotFoundException.class);
    }
}
