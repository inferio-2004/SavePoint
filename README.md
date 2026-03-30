# Savepoint

Savepoint is a video game review platform, similar in concept to Letterboxd but built for games. Users log games they have played, write reviews, and follow other people on the platform. What separates Savepoint from a generic review site is verification — when a user signs in through Steam, the platform confirms whether they actually own and have played a game before letting them review it. Reviews from verified players are marked as such, and a game's overall score is calculated only from those verified reviews. Users who sign up manually can still write reviews, they just will not carry the verified badge.

The social layer lets users follow each other and like reviews.


## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security (session-based, no JWT)
- PostgreSQL via Docker
- Elasticsearch (full-text game and user search)
- Redis (session store + caching)
- Lombok
- Jakarta Validation


## What has been built so far

**Authentication** is fully working. Users can sign in one of two ways — through Steam using OpenID 2.0, or manually with an email and password.

Steam authentication redirects the user to the Steam login page, handles the OpenID callback, verifies the response parameters directly with Steam's servers, extracts the Steam64 ID, fetches the user's display name and avatar from the Steam Web API, and creates an account automatically if one does not exist yet.

Manual authentication uses BCrypt password hashing and Spring Security's DaoAuthenticationProvider. Passwords are hashed before storage. The BCrypt 72-character limit is enforced at the validation layer so nothing gets silently truncated.

Both flows use session-based authentication with HttpOnly and SameSite=Strict cookies. Sessions are persisted to Redis via Spring Session so they survive server restarts.

The auth design is intentionally loosely coupled. The controller only talks to AuthService. AuthService only talks to AuthenticationManager. The actual verification logic for Steam lives in SteamAuthProvider, and manual login goes through DaoAuthenticationProvider. Adding a new auth method in the future — Google, Discord, whatever — means writing a new AuthenticationProvider and a new token class, with no changes to the controller or AuthService.

**User management** covers profile creation, login, signup, and the DTOs that shape what the API exposes. User identity is separated from auth credentials — a UserProfile holds display name and avatar, while UserAuth holds the provider type, the hashed password or Steam ID, and the email. This means a single user account can potentially be linked to multiple auth methods later.

**Game management** covers game persistence, IGDB integration, and Steam library onboarding. Games are pre-seeded from IGDB on startup. When a Steam user logs in, their owned games are imported asynchronously in the background without blocking the login response. Each game is looked up by Steam App ID, matched against the local database or fetched from IGDB, and linked to the user's library. A scheduled job syncs all Steam libraries every 24 hours.

**Search** is powered by Elasticsearch. Games and users are indexed as documents alongside their Postgres records (dual-write). Full-text search handles fuzzy matching and relevance ranking. A reindex endpoint allows rebuilding the ES indexes from Postgres at any time.

**Reviews** support a draft/publish workflow. A user can save a review as a draft as many times as they want before publishing. Verification status is determined once at creation time — a Steam-imported game yields a verified review badge, a manually added game does not. Published reviews notify the author's followers asynchronously.

**Social features** include follow/unfollow, liking reviews, and a notification system. Notifications are created for follows, likes, and new reviews from followed users. The follower fan-out on review publish is async so it does not block the publish response.

**Validation** is enforced on all incoming request bodies. Passwords must be between 8 and 72 characters. Emails must be valid. Usernames cannot be blank or longer than 50 characters. Validation failures return a 400 with a field-level error message.

**Global exception handling** is centralized in a single ControllerAdvice. All domain exceptions are mapped to appropriate HTTP status codes. Spring Security authentication failures are handled separately through the AuthenticationEntryPoint configured in the security filter chain.

**Tests** cover all layers — unit tests with Mockito for service logic, and integration tests with Spring Boot Test and Awaitility for async flows.


## What is coming next

- Elasticsearch + Redis integration (in progress)
- Custom `ThreadPoolTaskExecutor` for fine-grained async thread control
- Backlog endpoints with pagination
- Frontend (React)


## How to run

**Prerequisites**

- Java 17
- Maven
- Docker

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

Clone the repository and create a file at `src/main/resources/application-local.properties` with the following:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/SPDB
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

steam.api.key=your_steam_api_key
app.base.url=http://localhost:5000
frontend.url=http://localhost:3000

spring.elasticsearch.uris=http://localhost:9200
spring.data.redis.host=localhost
spring.data.redis.port=6379

server.port=5000
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

**Run the application**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on port 5000.

**Run the tests**

```bash
./mvnw test
```


## API Endpoints

### Auth (public)
```
GET  /auth/steam                        Redirect to Steam OpenID login
GET  /auth/steam/callback               Handle Steam OpenID callback
POST /auth/manual/signup                Register with email and password
POST /auth/manual/login                 Login with email and password
```

### Games
```
GET  /api/games/search?gameName=        Full-text game search via Elasticsearch
GET  /api/games/{id}                    Get game by ID
POST /api/games/seed                    Seed top games from IGDB (authenticated)
```

### Reviews
```
POST  /api/reviews/{gameId}             Save or update a draft review
PATCH /api/reviews/{gameId}/publish     Publish a review
GET   /api/reviews/game/{gameId}        List published reviews for a game (public)
GET   /api/reviews/me/{gameId}          Get your own review for a game
```

### Social
```
POST   /api/follow/{followeeId}         Follow a user
DELETE /api/follow/{followeeId}         Unfollow a user
GET    /api/follow/{userId}/followers   List followers (public)
GET    /api/follow/{userId}/following   List following (public)

POST   /api/likes/{reviewId}            Like a review
DELETE /api/likes/{reviewId}            Unlike a review
GET    /api/likes/{reviewId}/count      Get like count (public)

GET    /api/notifications               Get your notifications
PATCH  /api/notifications/{id}/read     Mark notification as read
PATCH  /api/notifications/read-all      Mark all notifications as read
```

### Backlog
```
GET  /api/gamelist                      Get your imported game library
```

### Admin
```
POST /api/admin/es/reindex              Rebuild Elasticsearch indexes from Postgres
```
