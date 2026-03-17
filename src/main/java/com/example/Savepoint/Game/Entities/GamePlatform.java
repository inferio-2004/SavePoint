package com.example.Savepoint.Game.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "game_platform")
public class GamePlatform {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String platformName;
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="game_id", nullable = false)
    @JsonIgnore
    private Game game;
}
