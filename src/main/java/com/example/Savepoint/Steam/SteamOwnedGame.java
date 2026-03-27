package com.example.Savepoint.Steam;

public record SteamOwnedGame(
        String appid,
        String name,
        Integer playtime_forever
) {}