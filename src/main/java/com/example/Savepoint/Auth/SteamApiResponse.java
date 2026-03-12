package com.example.Savepoint.Auth;

import java.util.List;

record PlayerSummary(String steamid, String personaname, String avatarfull) {}
record PlayersResponse(List<PlayerSummary> players) {}
record SteamApiResponse(PlayersResponse response) {}