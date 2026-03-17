package com.example.Savepoint.Game.Entities;

import com.example.Savepoint.User.UserProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "user_game",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"})
)
public class UserGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserGamePlatform platform;
    
    private Integer hoursPlayed;
    
    private LocalDateTime lastPlayedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus;
}
