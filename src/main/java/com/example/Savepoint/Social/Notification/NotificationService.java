package com.example.Savepoint.Social.Notification;

import com.example.Savepoint.Exceptions.NotificationNotFoundException;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.Exceptions.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserProfileJpaRepositry userProfileRepository;

    // Internal — called by FollowService and LikeService to push a single notification
    @Transactional
    public void createNotification(Integer recipientId, NotificationType type,
                                   Long referenceId, ReferenceType referenceType) {
        UserProfile recipient = userProfileRepository.findById(recipientId)
                .orElseThrow(() -> new BadCredentialsException("Recipient not found"));

        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notificationRepository.save(notification);
    }

    // Called by ReviewService.publishReview() — async because fan-out to all followers
    // can be expensive for users with large followings. Runs on a separate thread.
    // NOTE: This must be called from a separate bean (not ReviewService itself) to
    // ensure the @Async proxy is respected. Inject NotificationService into ReviewService.
    @Async
    @Transactional
    public void notifyFollowersOfNewReview(List<UserProfile> followers, Long reviewId) {
        List<Notification> notifications = followers.stream()
                .map(follower -> Notification.builder()
                        .user(follower)
                        .type(NotificationType.NEW_REVIEW)
                        .referenceId(reviewId)
                        .referenceType(ReferenceType.REVIEW)
                        .build()
                ).toList();

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Integer userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public void markAsRead(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationNotFoundException("Notification not found");
        }

        notification.setRead(true);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
