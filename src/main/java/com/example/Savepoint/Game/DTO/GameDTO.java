package com.example.Savepoint.Game.DTO;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public record GameDTO(
        Long id,
        String title,
        String coverThumb,
        LocalDate releaseDate,
        List<String> genres,
        List<String> developers,
        List<String> platforms
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
