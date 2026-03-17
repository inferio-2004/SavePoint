package com.example.Savepoint.Game.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique=true,nullable = false)
    private Integer igdbId;
    @Column(nullable=false)
    private String title;
    private String description;
    private String coverThumb;
    private String coverBig;
    private String cover1080p;
    private LocalDate releaseDate;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GameGenre> gameGenres = new HashSet<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GameDeveloper> gameDevelopers = new HashSet<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = ALL, orphanRemoval = true)
    private Set<GamePlatform> gamePlatforms=new HashSet<>();
    
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = ALL, orphanRemoval = true)
    private Set<GamePlatformId> gamePlatformIds = new HashSet<>();
}
