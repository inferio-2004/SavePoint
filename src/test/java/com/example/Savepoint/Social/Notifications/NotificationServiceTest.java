package com.example.Savepoint.Social.Notifications;

import com.example.Savepoint.Exceptions.NotificationNotFoundException;
import com.example.Savepoint.Social.Notification.*;
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
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;
    @Mock
    UserProfileJpaRepositry userProfileRepository;

    @InjectMocks
    NotificationService notificationService;

    private UserProfile user;
    private final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        user = UserProfile.builder().id(USER_ID).displayName("testuser").build();
    }

    @Test
    void createNotification_happyPath_savesNotification() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(USER_ID, NotificationType.FOLLOW, 2L, ReferenceType.USER);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.FOLLOW &&
                n.getReferenceId().equals(2L) &&
                n.getReferenceType() == ReferenceType.USER &&
                !n.isRead()
        ));
    }

    @Test
    void createNotification_userNotFound_throwsUserNotFoundException() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.createNotification(USER_ID, NotificationType.LIKE, 5L, ReferenceType.REVIEW)
        ).isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void notifyFollowersOfNewReview_savesOneNotificationPerFollower() {
        UserProfile follower1 = UserProfile.builder().id(10).displayName("follower1").build();
        UserProfile follower2 = UserProfile.builder().id(11).displayName("follower2").build();
        Long reviewId = 99L;

        notificationService.notifyFollowersOfNewReview(List.of(follower1, follower2), reviewId);

        verify(notificationRepository).saveAll(argThat(notifications -> {
            List<Notification> list = (List<Notification>) notifications;
            return list.size() == 2 &&
                   list.stream().allMatch(n ->
                       n.getType() == NotificationType.NEW_REVIEW &&
                       n.getReferenceId().equals(reviewId) &&
                       n.getReferenceType() == ReferenceType.REVIEW
                   );
        }));
    }

    @Test
    void notifyFollowersOfNewReview_noFollowers_savesEmptyList() {
        notificationService.notifyFollowersOfNewReview(List.of(), 99L);

        verify(notificationRepository).saveAll(argThat(notifications ->
                ((List<Notification>) notifications).isEmpty()
        ));
    }

    @Test
    void getNotifications_returnsMappedDTOs() {
        Notification notification = Notification.builder()
                .id(1L).user(user)
                .type(NotificationType.LIKE)
                .referenceId(5L)
                .referenceType(ReferenceType.REVIEW)
                .isRead(false)
                .build();

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(eq(USER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationDTO> result = notificationService.getNotifications(USER_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).type()).isEqualTo(NotificationType.LIKE);
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }

    @Test
    void markAsRead_ownerMarksNotification_setsReadTrue() {
        Notification notification = Notification.builder()
                .id(1L).user(user).isRead(false).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, USER_ID);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_wrongUser_throwsNotificationNotFoundException() {
        UserProfile otherUser = UserProfile.builder().id(99).build();
        Notification notification = Notification.builder()
                .id(1L).user(otherUser).isRead(false).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // User ID 1 tries to mark a notification belonging to user 99
        assertThatThrownBy(() -> notificationService.markAsRead(1L, USER_ID))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllAsRead_delegatesToRepository() {
        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).markAllAsReadByUserId(USER_ID);
    }
}
