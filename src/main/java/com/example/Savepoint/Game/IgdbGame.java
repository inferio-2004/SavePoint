package com.example.Savepoint.Game;

import java.util.List;
public record IgdbGame(
        Integer id,
        RawCover cover,
        List<Genre> genres,
        String name,
        String summary,
        Long first_release_date,
        List<InvolvedCompany> involved_companies,
        List<Platform> platforms
) {
    public record RawCover(String url) {}
    public record Genre( String name) {}
    public record Company(String name) {}
    public record InvolvedCompany(Company company, boolean developer) {}
    public record Platform(String name) {}
}