# Savepoint

Savepoint is a video game review platform, similar in concept to Letterboxd but built for games. The idea is straightforward — users log games they have played, write reviews, and follow other people on the platform. What separates Savepoint from a generic review site is verification. When a user signs in through Steam, the platform can confirm whether they actually own and have played a game before letting them review it. Reviews from verified players are marked as such, and a game's overall score is calculated only from those verified reviews. Users who sign up manually can still write reviews, they just will not carry the verified badge.

The social layer lets users follow each other and like reviews.


## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security (session-based, no JWT)
- PostgreSQL via Docker
- Lombok
- Jakarta Validation


## What has been built so far

**Authentication** is fully working. Users can sign in one of two ways — through Steam using OpenID 2.0, or manually with an email and password.

Steam authentication redirects the user to the Steam login page, handles the OpenID callback, verifies the response parameters directly with Steam's servers, extracts the Steam64 ID, fetches the user's display name and avatar from the Steam Web API, and creates an account automatically if one does not exist yet.

Manual authentication uses BCrypt password hashing and Spring Security's DaoAuthenticationProvider. Passwords are hashed before storage. The BCrypt 72-character limit is enforced at the validation layer so nothing gets silently truncated.

Both flows use session-based authentication with HttpOnly and SameSite=Strict cookies. Sessions are saved to the HTTP session via HttpSessionSecurityContextRepository.

The auth design is intentionally loosely coupled. The controller only talks to AuthService. AuthService only talks to AuthenticationManager. The actual verification logic for Steam lives in SteamAuthProvider, and manual login goes through DaoAuthenticationProvider. Adding a new auth method in the future — Google, Discord, whatever — means writing a new AuthenticationProvider and a new token class, with no changes to the controller or AuthService.

**User management** covers profile creation, login, signup, and the DTOs that shape what the API exposes. User identity is separated from auth credentials — a UserProfile holds display name and avatar, while UserAuth holds the provider type, the hashed password or Steam ID, and the email. This means a single user account can potentially be linked to multiple auth methods later.

**Validation** is enforced on all incoming request bodies. Passwords must be between 8 and 72 characters. Emails must be valid. Usernames cannot be blank or longer than 50 characters. Validation failures return a 400 with a field-level error message.

**Global exception handling** is centralized in a single ControllerAdvice. Handled cases include duplicate email on signup (409), invalid Steam credentials (401), Steam user not found (404), request validation failures (400), Steam API being unreachable (503), and general database errors (503). Spring Security authentication failures are handled separately through the AuthenticationEntryPoint configured in the security filter chain.

**Tests** cover all three layers. AuthServiceTest tests the service logic in isolation using Mockito — redirect URL construction, Steam callback delegation, manual login flow, and registration logic including the check that the hashed password (not the plain text one) is what gets stored. SteamAuthProviderTest tests the Steam OpenID verification, existing user lookup, new user creation via the Steam API, and failure cases. UserControllerTest uses WebMvcTest with the real SecurityConfig imported so the actual permit rules are applied, testing all endpoints for correct status codes, validation rejection, and exception mapping.


## What is coming next

- Game entity and Steam game search integration
- Review system with verification status based on Steam ownership data
- Follow and like functionality
- Async Steam library import on login so the user's backlog is populated in the background without blocking the login response
- Scheduled Steam library sync
- Backlog endpoints with pagination
- Notification system


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

**Configuration**

Clone the repository and create a file at `src/main/resources/application-local.properties` with the following:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/SPDB
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

steam.api.key=your_steam_api_key
app.base.url=http://localhost:5000
frontend.url=http://localhost:3000

server.port=5000
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

Make sure the database name, username, and password match what you passed to the Docker container. You can get a Steam API key at steamcommunity.com/dev/apikey. Use localhost as the domain when registering for a dev key.

**Run the application**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on port 5000.

**Run the tests**

```bash
./mvnw test
```


## Auth endpoints

```
GET  /auth/steam                  Redirects to Steam OpenID login
GET  /auth/steam/callback         Handles Steam OpenID callback after login
POST /auth/manual/signup          Register a new account with email and password
POST /auth/manual/login           Login with email and password
```

All auth endpoints are public. Everything else requires an active session.
