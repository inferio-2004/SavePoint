package com.example.Savepoint.Game.DTO;

import com.example.Savepoint.Game.Entities.ReviewStatus;
import com.example.Savepoint.Game.Entities.UserGamePlatform;
import com.example.Savepoint.Review.ReviewDTO;

public record UserLibraryDTO(
        Long gameId,
        String title,
        String coverThumb,
        UserGamePlatform platform,
        ReviewStatus reviewStatus,
        ReviewDTO review  // null if UNREVIEWED
) {}
