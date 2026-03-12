# Savepoint

Savepoint is a video game review platform, similar in concept to Letterboxd but built for games. The core idea is simple — users can log games they have played, write reviews, and follow other users. What makes Savepoint different is that reviews are verified. If a user signs in with Steam, the platform can confirm whether they actually played the game before letting them review it, which cuts down on review bombing and low-effort takes.

The social side of the platform lets users follow each other, like reviews, and get notified when someone they follow posts something new.

## What has been built so far

The backend is built with Spring Boot and PostgreSQL.

**Authentication** is fully working. Users can sign in one of two ways — through Steam using OpenID 2.0, or manually with an email and password. Steam authentication verifies the OpenID callback parameters, fetches the user's display name and avatar from the Steam API, and creates an account if one does not already exist. Manual authentication uses BCrypt password hashing and Spring Security's DaoAuthenticationProvider. Both flows use session-based authentication with HttpOnly, SameSite=Strict cookies.

**User management** covers profile creation, login, signup, and the data transfer objects that shape what the API exposes to the client.

**Global exception handling** is in place via a @ControllerAdvice that intercepts domain exceptions and maps them to consistent JSON error responses. Handled cases include duplicate email on signup, invalid Steam credentials, Steam user not found, validation failures on request bodies, and Steam API being unreachable. Spring Security authentication failures are handled separately through a configured AuthenticationEntryPoint in the security filter chain.

## What is coming next

- Game entity and Steam game search integration
- Review system with verification status
- Follow and like functionality
- Notification system
- Async Steam game import on login so the backlog is populated in the background without blocking the response
- Backlog endpoints with pagination
- Scheduled Steam library sync every 24 hours

## How to run

**Prerequisites**

- Java 21
- Maven
- Docker

**Start the database**

Run a PostgreSQL container:

```bash
docker run --name savepoint-db \
  -e POSTGRES_DB=savepoint \
  -e POSTGRES_USER=your_db_username \
  -e POSTGRES_PASSWORD=your_db_password \
  -p 5432:5432 \
  -d postgres
```

**Setup**

Clone the repository and create a file at `src/main/resources/application-local.properties` with the following:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/savepoint
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

steam.api.key=your_steam_api_key
app.base.url=http://localhost:8080

server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
server.servlet.session.cookie.secure=false
```

Make sure the username and password here match what you passed to the Docker container above.

You can get a Steam API key at steamcommunity.com/dev/apikey. Use localhost as the domain when developing locally.

**Running the application**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on port 5000.

**Auth endpoints**

```
GET  /auth/steam               Redirects to Steam login
GET  /auth/steam/callback      Handles Steam OpenID callback
POST /auth/manual/signup       Register with email and password
POST /auth/manual/login        Login with email and password
```
