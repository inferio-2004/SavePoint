package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Auth.SessionAuthenticationToken;
import com.example.Savepoint.Game.DTO.UserGameDTO;
import com.example.Savepoint.Game.DTO.UserLibraryDTO;
import com.example.Savepoint.Game.Entities.UserGame;
import com.example.Savepoint.Game.Repositories.UserGameRepository;
import com.example.Savepoint.Review.Review;
import com.example.Savepoint.Review.ReviewDTO;
import com.example.Savepoint.Review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserGameService {
    private final UserGameRepository userGameRepository;
    private final ReviewRepository reviewRepository;

    public Page<UserGameDTO> getUserGames(Integer userId,Pageable pageable) {
        return userGameRepository.findByUser_Id(userId,pageable).map(this::toDTO);
    }

    public Page<UserLibraryDTO> getOwnLibrary(Integer userId, Pageable pageable) {
        return userGameRepository.findByUser_Id(userId, pageable)
                .map(userGame -> {
                    ReviewDTO reviewDTO = reviewRepository
                            .findByUser_IdAndGame_Id(userId, userGame.getGame().getId())
                            .map(this::toReviewDTO)
                            .orElse(null);
                    return new UserLibraryDTO(
                            userGame.getGame().getId(),
                            userGame.getGame().getTitle(),
                            userGame.getGame().getCoverThumb(),
                            userGame.getPlatform(),
                            userGame.getReviewStatus(),
                            reviewDTO
                    );
                });
    }

    public UserGameDTO toDTO(UserGame userGame){
        return new UserGameDTO(userGame.getGame().getId(),
                userGame.getGame().getTitle(),
                userGame.getGame().getCoverThumb(),
                userGame.getPlatform(),
                userGame.getHoursPlayed(),
                userGame.getLastPlayedAt(),
                userGame.getReviewStatus());
    }

    private ReviewDTO toReviewDTO(Review review) {
        return new ReviewDTO(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getDisplayName(),
                review.getGame().getId(),
                review.getGame().getTitle(),
                review.getRating(),
                review.getReviewText(),
                review.isVerified(),
                review.isPublished(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
