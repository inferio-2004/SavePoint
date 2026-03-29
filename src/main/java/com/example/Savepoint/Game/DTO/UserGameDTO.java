package com.example.Savepoint.Game.DTO;

import com.example.Savepoint.Game.Entities.ReviewStatus;
import com.example.Savepoint.Game.Entities.UserGamePlatform;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record UserGameDTO(Long gameId, String title, String coverThumb,
                          UserGamePlatform platform, @Nullable Integer hoursPlayed,
                          @Nullable LocalDateTime lastPlayedAt, ReviewStatus reviewStatus) {
}
