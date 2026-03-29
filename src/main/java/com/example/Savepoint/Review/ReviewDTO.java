package com.example.Savepoint.Review.DTO;

import java.time.LocalDateTime;

public record ReviewDTO(
        Long id,
        Integer userId,
        String displayName,
        Long gameId,
        String gameTitle,
        Integer rating,
        String reviewText,
        boolean isVerified,
        boolean isPublished,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
