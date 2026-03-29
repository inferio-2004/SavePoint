package com.example.Savepoint.Social.Like;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUser_IdAndReview_Id(Integer userId, Long reviewId);

    boolean existsByUser_IdAndReview_Id(Integer userId, Long reviewId);

    long countByReview_Id(Long reviewId);
}
