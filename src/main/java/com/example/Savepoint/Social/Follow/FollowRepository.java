package com.example.Savepoint.Social.Follow;

import com.example.Savepoint.User.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollower_IdAndFollowee_Id(Integer followerId, Integer followeeId);

    boolean existsByFollower_IdAndFollowee_Id(Integer followerId, Integer followeeId);

    Page<Follow> findByFollowee_Id(Integer followeeId, Pageable pageable);  // followers of a user

    Page<Follow> findByFollower_Id(Integer followerId, Pageable pageable);  // users a person follows

    // Used by NotificationService to fan-out NEW_REVIEW notifications
    @Query("SELECT f.follower FROM Follow f WHERE f.followee.id = :userId")
    List<UserProfile> findFollowersByUserId(@Param("userId") Integer userId);
}
