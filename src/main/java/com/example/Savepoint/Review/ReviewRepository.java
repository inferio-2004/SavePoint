package com.example.Savepoint.Review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUser_IdAndGame_Id(Integer userId, Long gameId);

    // Used for public-facing review listing — drafts excluded
    Page<Review> findByGame_IdAndIsPublishedTrue(Long gameId, Pageable pageable);

    // Used for a user's own profile/backlog page — includes drafts
    Page<Review> findByUser_Id(Integer userId, Pageable pageable);
}
