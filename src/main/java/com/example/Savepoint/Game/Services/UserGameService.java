package com.example.Savepoint.Game.Services;

import com.example.Savepoint.Auth.SessionAuthenticationToken;
import com.example.Savepoint.Game.DTO.UserGameDTO;
import com.example.Savepoint.Game.Entities.UserGame;
import com.example.Savepoint.Game.Repositories.UserGameRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
public class UserGameService {
    private final UserGameRepository userGameRepository;

    public UserGameService(UserGameRepository userGameRepository) {
        this.userGameRepository = userGameRepository;
    }

    public Page<UserGameDTO> getUserGames(Integer userId,Pageable pageable) {
        return userGameRepository.findByUser_Id(userId,pageable).map(this::toDTO);
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
}
