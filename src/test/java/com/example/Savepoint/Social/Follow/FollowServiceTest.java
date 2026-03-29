package com.example.Savepoint.Social.Follow;

import com.example.Savepoint.Exceptions.AlreadyFollowingException;
import com.example.Savepoint.Exceptions.NotFollowingException;
import com.example.Savepoint.Exceptions.SelfFollowException;
import com.example.Savepoint.Social.Notification.NotificationService;
import com.example.Savepoint.Social.Notification.NotificationType;
import com.example.Savepoint.Social.Notification.ReferenceType;
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
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock
    UserProfileJpaRepositry userProfileRepository;
    @Mock NotificationService notificationService;

    @InjectMocks FollowService followService;

    private UserProfile follower;
    private UserProfile followee;
    private final Integer FOLLOWER_ID = 1;
    private final Integer FOLLOWEE_ID = 2;

    @BeforeEach
    void setUp() {
        follower = UserProfile.builder().id(FOLLOWER_ID).displayName("follower").build();
        followee = UserProfile.builder().id(FOLLOWEE_ID).displayName("followee").build();
    }

    @Test
    void follow_happyPath_createsFollowAndNotification() {
        when(followRepository.existsByFollower_IdAndFollowee_Id(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(false);
        when(userProfileRepository.findById(FOLLOWER_ID)).thenReturn(Optional.of(follower));
        when(userProfileRepository.findById(FOLLOWEE_ID)).thenReturn(Optional.of(followee));
        when(followRepository.save(any(Follow.class))).thenAnswer(inv -> inv.getArgument(0));

        followService.follow(FOLLOWER_ID, FOLLOWEE_ID);

        verify(followRepository).save(argThat(f ->
                f.getFollower().getId().equals(FOLLOWER_ID) &&
                f.getFollowee().getId().equals(FOLLOWEE_ID)
        ));
        verify(notificationService).createNotification(
                FOLLOWEE_ID,
                NotificationType.FOLLOW,
                FOLLOWER_ID.longValue(),
                ReferenceType.USER
        );
    }

    @Test
    void follow_selfFollow_throwsSelfFollowException() {
        assertThatThrownBy(() -> followService.follow(FOLLOWER_ID, FOLLOWER_ID))
                .isInstanceOf(SelfFollowException.class);

        verifyNoInteractions(followRepository, notificationService);
    }

    @Test
    void follow_alreadyFollowing_throwsAlreadyFollowingException() {
        when(followRepository.existsByFollower_IdAndFollowee_Id(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> followService.follow(FOLLOWER_ID, FOLLOWEE_ID))
                .isInstanceOf(AlreadyFollowingException.class);

        verify(followRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void follow_followeeNotFound_throwsUserNotFoundException() {
        when(followRepository.existsByFollower_IdAndFollowee_Id(FOLLOWER_ID, FOLLOWEE_ID)).thenReturn(false);
        when(userProfileRepository.findById(FOLLOWER_ID)).thenReturn(Optional.of(follower));
        when(userProfileRepository.findById(FOLLOWEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.follow(FOLLOWER_ID, FOLLOWEE_ID))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unfollow_existingFollow_deletesFollow() {
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        when(followRepository.findByFollower_IdAndFollowee_Id(FOLLOWER_ID, FOLLOWEE_ID))
                .thenReturn(Optional.of(follow));

        followService.unfollow(FOLLOWER_ID, FOLLOWEE_ID);

        verify(followRepository).delete(follow);
    }

    @Test
    void unfollow_notFollowing_throwsNotFollowingException() {
        when(followRepository.findByFollower_IdAndFollowee_Id(FOLLOWER_ID, FOLLOWEE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollow(FOLLOWER_ID, FOLLOWEE_ID))
                .isInstanceOf(NotFollowingException.class);
    }

    @Test
    void getFollowers_returnsPageOfFollowers() {
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        when(followRepository.findByFollowee_Id(eq(FOLLOWEE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(follow)));

        Page<FollowDTO> result = followService.getFollowers(FOLLOWEE_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(FOLLOWER_ID);
    }

    @Test
    void getFollowing_returnsPageOfFollowees() {
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        when(followRepository.findByFollower_Id(eq(FOLLOWER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(follow)));

        Page<FollowDTO> result = followService.getFollowing(FOLLOWER_ID, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(FOLLOWEE_ID);
    }
}
