package com.example.Savepoint.Social.Like;

import com.example.Savepoint.Exceptions.AlreadyLikedException;
import com.example.Savepoint.Exceptions.NotLikedException;
import com.example.Savepoint.Review.Review;
import com.example.Savepoint.Exceptions.ReviewNotFoundException;
import com.example.Savepoint.Review.ReviewRepository;
import com.example.Savepoint.Social.Notification.NotificationService;
import com.example.Savepoint.Social.Notification.NotificationType;
import com.example.Savepoint.Social.Notification.ReferenceType;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock UserProfileJpaRepositry userProfileRepository;
    @Mock NotificationService notificationService;

    @InjectMocks LikeService likeService;

    private UserProfile liker;
    private UserProfile reviewOwner;
    private Review review;
    private final Integer LIKER_ID = 1;
    private final Integer OWNER_ID = 2;
    private final Long REVIEW_ID = 10L;

    @BeforeEach
    void setUp() {
        liker = UserProfile.builder().id(LIKER_ID).displayName("liker").build();
        reviewOwner = UserProfile.builder().id(OWNER_ID).displayName("owner").build();
        review = Review.builder().id(REVIEW_ID).user(reviewOwner).build();
    }

    @Test
    void likeReview_happyPath_createsLikeAndNotifiesOwner() {
        when(likeRepository.existsByUser_IdAndReview_Id(LIKER_ID, REVIEW_ID)).thenReturn(false);
        when(userProfileRepository.findById(LIKER_ID)).thenReturn(Optional.of(liker));
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(likeRepository.save(any(Like.class))).thenAnswer(inv -> inv.getArgument(0));

        likeService.likeReview(LIKER_ID, REVIEW_ID);

        verify(likeRepository).save(argThat(l ->
                l.getUser().getId().equals(LIKER_ID) &&
                l.getReview().getId().equals(REVIEW_ID)
        ));
        verify(notificationService).createNotification(
                OWNER_ID,
                NotificationType.LIKE,
                REVIEW_ID,
                ReferenceType.REVIEW
        );
    }

    @Test
    void likeReview_selfLike_noNotificationSent() {
        // Owner likes their own review — like is saved but no notification triggered
        UserProfile ownerAsLiker = UserProfile.builder().id(OWNER_ID).build();
        when(likeRepository.existsByUser_IdAndReview_Id(OWNER_ID, REVIEW_ID)).thenReturn(false);
        when(userProfileRepository.findById(OWNER_ID)).thenReturn(Optional.of(ownerAsLiker));
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        when(likeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        likeService.likeReview(OWNER_ID, REVIEW_ID);

        verify(likeRepository).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void likeReview_alreadyLiked_throwsAlreadyLikedException() {
        when(likeRepository.existsByUser_IdAndReview_Id(LIKER_ID, REVIEW_ID)).thenReturn(true);

        assertThatThrownBy(() -> likeService.likeReview(LIKER_ID, REVIEW_ID))
                .isInstanceOf(AlreadyLikedException.class);

        verify(likeRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void likeReview_reviewNotFound_throwsReviewNotFoundException() {
        when(likeRepository.existsByUser_IdAndReview_Id(LIKER_ID, REVIEW_ID)).thenReturn(false);
        when(userProfileRepository.findById(LIKER_ID)).thenReturn(Optional.of(liker));
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.likeReview(LIKER_ID, REVIEW_ID))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void unlikeReview_existingLike_deletesLike() {
        Like like = Like.builder().user(liker).review(review).build();
        when(likeRepository.findByUser_IdAndReview_Id(LIKER_ID, REVIEW_ID)).thenReturn(Optional.of(like));

        likeService.unlikeReview(LIKER_ID, REVIEW_ID);

        verify(likeRepository).delete(like);
    }

    @Test
    void unlikeReview_notLiked_throwsNotLikedException() {
        when(likeRepository.findByUser_IdAndReview_Id(LIKER_ID, REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.unlikeReview(LIKER_ID, REVIEW_ID))
                .isInstanceOf(NotLikedException.class);
    }

    @Test
    void getLikeCount_returnsCorrectCount() {
        when(likeRepository.countByReview_Id(REVIEW_ID)).thenReturn(42L);

        long count = likeService.getLikeCount(REVIEW_ID);

        assertThat(count).isEqualTo(42L);
    }
}
