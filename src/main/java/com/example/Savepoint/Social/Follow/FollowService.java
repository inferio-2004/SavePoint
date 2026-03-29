package com.example.Savepoint.Social.Follow;

import com.example.Savepoint.Exceptions.AlreadyFollowingException;
import com.example.Savepoint.Exceptions.NotFollowingException;
import com.example.Savepoint.Exceptions.SelfFollowException;
import com.example.Savepoint.Social.Notification.NotificationService;
import com.example.Savepoint.Social.Notification.NotificationType;
import com.example.Savepoint.Social.Notification.ReferenceType;
import com.example.Savepoint.User.UserProfile;
import com.example.Savepoint.User.UserProfileJpaRepositry;
import com.example.Savepoint.Exceptions.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserProfileJpaRepositry userProfileRepository;
    private final NotificationService notificationService;

    @Transactional
    public void follow(Integer followerId, Integer followeeId) {
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException();
        }

        if (followRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId)) {
            throw new AlreadyFollowingException("Already following this user");
        }

        UserProfile follower = userProfileRepository.findById(followerId)
                .orElseThrow(() -> new BadCredentialsException("Follower not found"));
        UserProfile followee = userProfileRepository.findById(followeeId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        followRepository.save(Follow.builder()
                .follower(follower)
                .followee(followee)
                .build());

        // Notify the followee — referenceId is the followerId so the UI can link to their profile
        notificationService.createNotification(
                followeeId,
                NotificationType.FOLLOW,
                followerId.longValue(),
                ReferenceType.USER
        );
    }

    @Transactional
    public void unfollow(Integer followerId, Integer followeeId) {
        Follow follow = followRepository.findByFollower_IdAndFollowee_Id(followerId, followeeId)
                .orElseThrow(() -> new NotFollowingException("Not following this user"));

        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> getFollowers(Integer userId, Pageable pageable) {
        return followRepository.findByFollowee_Id(userId, pageable)
                .map(f -> toDTO(f.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<FollowDTO> getFollowing(Integer userId, Pageable pageable) {
        return followRepository.findByFollower_Id(userId, pageable)
                .map(f -> toDTO(f.getFollowee()));
    }

    // Used internally by NotificationService fan-out on new review publish
    public List<UserProfile> getFollowers(Integer userId) {
        return followRepository.findFollowersByUserId(userId);
    }

    private FollowDTO toDTO(UserProfile user) {
        return new FollowDTO(user.getId(), user.getDisplayName(), user.getAvatarUrl());
    }
}
