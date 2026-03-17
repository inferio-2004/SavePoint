package com.example.Savepoint.Game.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Developer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String name;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "developer", cascade = CascadeType.ALL, orphanRemoval=true)
    private Set<GameDeveloper> gameDevelopers = new HashSet<>();
}
