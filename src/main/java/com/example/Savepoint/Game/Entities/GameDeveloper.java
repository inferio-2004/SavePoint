package com.example.Savepoint.Game.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "game_developer")
public class GameDeveloper {
    @EmbeddedId
    @Builder.Default
    private GameDeveloperId id=new GameDeveloperId();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("developerId")
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;
}
