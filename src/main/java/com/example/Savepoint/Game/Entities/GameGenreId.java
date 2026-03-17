package com.example.Savepoint.Game.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class GameGenreId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "genre_id")
    private Long genreId;
}

