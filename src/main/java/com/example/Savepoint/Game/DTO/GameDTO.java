package com.example.Savepoint.Game.DTO;

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
) {}