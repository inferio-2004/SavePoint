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
import com.example.Savepoint.Exceptions.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final UserProfileJpaRepositry userProfileRepository;
    private final NotificationService notificationService;

    @CacheEvict(value = "likeCounts", key = "#reviewId")
    @Transactional
    public void likeReview(Integer userId, Long reviewId) {
        if (likeRepository.existsByUser_IdAndReview_Id(userId, reviewId)) {
            throw new AlreadyLikedException("Already liked this review");
        }

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        // Users shouldn't be notified when they like their own review
        boolean isSelfLike = review.getUser().getId().equals(userId);

        likeRepository.save(Like.builder()
                .user(user)
                .review(review)
                .build());

        if (!isSelfLike) {
            notificationService.createNotification(
                    review.getUser().getId(),
                    NotificationType.LIKE,
                    reviewId,
                    ReferenceType.REVIEW
            );
        }
    }

    @CacheEvict(value = "likeCounts", key = "#reviewId")
    @Transactional
    public void unlikeReview(Integer userId, Long reviewId) {
        Like like = likeRepository.findByUser_IdAndReview_Id(userId, reviewId)
                .orElseThrow(() -> new NotLikedException("Review was not liked"));

        likeRepository.delete(like);
    }

    @Cacheable(value = "likeCounts", key = "#reviewId")
    @Transactional(readOnly = true)
    public long getLikeCount(Long reviewId) {
        return likeRepository.countByReview_Id(reviewId);
    }
}
