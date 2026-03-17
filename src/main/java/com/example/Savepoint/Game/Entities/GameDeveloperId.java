package com.example.Savepoint.Game.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class GameDeveloperId implements Serializable {
    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "developer_id")
    private Long developerId;
}
