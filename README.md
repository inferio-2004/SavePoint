# Savepoint

Savepoint is a video game review platform in the spirit of Letterboxd, built for games. Users log games they've played, write reviews, and follow others on the platform. The defining feature is verification — when a user signs in through Steam, the platform confirms whether they actually own and have played a game before letting them review it. Reviews from verified players carry a verified badge, and the overall score for a game is derived from those reviews. Users who sign up manually can still write reviews; they just won't carry the badge.

The social layer supports follows, likes, and a notification inbox. Search is powered by Elasticsearch with fuzzy matching. Sessions are backed by Redis.

---

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security (session-based, no JWT)
- PostgreSQL via Docker
- Elasticsearch (full-text game and user search)
- Redis (session store + caching)
- Lombok
- Jakarta Validation

---

## What Has Been Built

### Authentication

Both Steam and manual auth are fully working.

Steam authentication redirects the user to the Steam login page, handles the OpenID 2.0 callback, verifies the response parameters directly with Steam's servers, extracts the Steam64 ID, fetches the user's display name and avatar from the Steam Web API, and creates an account automatically if one doesn't exist.

Manual authentication uses BCrypt password hashing and Spring Security's `DaoAuthenticationProvider`. The BCrypt 72-character limit is enforced at the validation layer.

Both flows use session-based authentication with `HttpOnly` and `SameSite=Strict` cookies. Sessions are persisted to Redis via Spring Session so they survive server restarts.

The design is loosely coupled by intention. The controller talks only to `AuthService`. `AuthService` talks only to `AuthenticationManager`. Verification logic for Steam lives in `SteamAuthProvider`; manual login goes through `DaoAuthenticationProvider`. Adding a new auth provider means writing a new `AuthenticationProvider` and token class — no changes to the controller or `AuthService`.

### User Management

User identity is separated from auth credentials. `UserProfile` holds display name, avatar, and role. `UserAuth` holds the provider type, hashed password or Steam ID, and email. A single account can be linked to multiple auth methods later.

### Roles

Users are assigned a role at registration: `USER` or `ADMIN`. Admin-only endpoints are protected via `.hasRole("ADMIN")` in the security filter chain. The `AdminController` exposes privileged operations like reindexing Elasticsearch and seeding games.

### Game Management

Games are seeded from IGDB on startup — the top 1000 by rating count, fetched in two batches of 500. When a Steam user logs in, their owned games are imported asynchronously in the background without blocking the login response. Each game is looked up by Steam App ID, matched against the local database or fetched from IGDB, and linked to the user's library. A scheduled job syncs all Steam libraries every 24 hours.

Race conditions during concurrent game inserts are handled via `GamePersistenceHelper`, which wraps inserts in `REQUIRES_NEW` transactions and falls back to a fetch on `DataIntegrityViolationException`.

### Search

Search is powered by Elasticsearch. Games and users are indexed as documents alongside their Postgres records — dual-write on create, with a full reindex endpoint available for recovery. Game search uses a multi-match query across `title` (boosted 3×) and `description` with `fuzziness: AUTO`. Users are searchable by display name.

### Reviews

Reviews support a draft/publish workflow. A user can save and update a draft as many times as they want before publishing. Verification status is determined once at creation time — a Steam-imported `UserGame` yields a verified badge; a manually added one does not. Published reviews trigger async notifications to the author's followers. `isPublished` is denormalized onto the `Review` entity to avoid joins on public listing queries.

### Social

Follow/unfollow, like/unlike, and a notification system are all implemented. Notifications are created for follows, likes, and new reviews from followed users. Follower fan-out on review publish is async so it doesn't block the publish response. Self-likes are permitted but don't trigger notifications.

### Caching

Redis is used for three caches with distinct TTLs:

| Cache | TTL |
|---|---|
| `games` | 6 hours |
| `publishedReviews` | 5 minutes |
| `likeCounts` | 2 minutes |

Cache eviction is handled at the service layer via `@CacheEvict` on mutating operations.

### Validation and Error Handling

All incoming request bodies are validated. Passwords must be 8–72 characters. Emails must be valid. Usernames cannot be blank or exceed 50 characters. Validation failures return a 400 with field-level messages.

Exception handling is centralized in a single `@RestControllerAdvice`. Domain exceptions map to appropriate HTTP status codes. Spring Security authentication failures are handled separately through the `AuthenticationEntryPoint` in the security filter chain, returning a consistent `{ status, message }` JSON body.

### Tests

Unit tests cover all service classes using `@ExtendWith(MockitoExtension.class)` and Mockito. Integration tests cover the game persistence race condition, the Steam onboarding flow end-to-end, and repository behaviour — using `@SpringBootTest` and `@DataJpaTest` respectively, with Awaitility for async assertions.

---

## What Is Coming Next

- Frontend (React)

---

## How to Run

**Prerequisites:** Java 17, Maven, Docker

**Start the database**

```bash
docker run --name savepoint-db \
  -e POSTGRES_DB=SPDB \
  -e POSTGRES_USER=your_db_username \
  -e POSTGRES_PASSWORD=your_db_password \
  -p 5432:5432 \
  -d postgres
```

**Start Elasticsearch**

```bash
docker run -d --name savepoint-es \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -p 9200:9200 \
  -v es-data:/usr/share/elasticsearch/data \
  elasticsearch:8.13.0
```

**Start Redis**

```bash
docker run -d --name savepoint-redis \
  -p 6379:6379 \
  redis:7
```

**Configuration**

Create `src/main/resources/application-local.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/SPDB
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

steam.api.key=your_steam_api_key
app.base.url=http://localhost:5000
frontend.url=http://localhost:3000

igdb.client-id=your_igdb_client_id
igdb.client-secret=your_igdb_client_secret

spring.elasticsearch.uris=http://localhost:9200
spring.data.redis.host=localhost
spring.data.redis.port=6379

server.port=5000
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Run**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Server starts on port 5000.

**Test**

```bash
./mvnw test
```

---

## API Reference

### Auth (public)

```
GET  /auth/steam                    Redirect to Steam OpenID login
GET  /auth/steam/callback           Handle Steam OpenID callback
POST /auth/manual/signup            Register with email and password
POST /auth/manual/login             Login with email and password
```

### Games

```
GET  /api/games/search?gameName=    Full-text search via Elasticsearch
GET  /api/games/{id}                Get game by internal ID
```

### Reviews

```
POST   /api/reviews/{gameId}            Save or update a draft
PATCH  /api/reviews/{gameId}/publish    Publish a review
GET    /api/reviews/game/{gameId}       List published reviews for a game (public)
GET    /api/reviews/me/{gameId}         Get your own review (includes draft)
```

### Social

```
POST   /api/follow/{followeeId}              Follow a user
DELETE /api/follow/{followeeId}              Unfollow a user
GET    /api/follow/{userId}/followers        List followers (public)
GET    /api/follow/{userId}/following        List following (public)

POST   /api/likes/{reviewId}                 Like a review
DELETE /api/likes/{reviewId}                 Unlike a review
GET    /api/likes/{reviewId}/count           Like count (public)

GET    /api/notifications                    Get your notifications
PATCH  /api/notifications/{id}/read          Mark one as read
PATCH  /api/notifications/read-all           Mark all as read
```

### Backlog

```
GET  /api/gamelist    Your imported game library (paginated)
```

### Admin (requires ADMIN role)

```
POST /api/admin/es/reindex     Rebuild Elasticsearch indexes from Postgres
POST /api/admin/games/seed     Seed top 1000 games from IGDB
```