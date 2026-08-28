# Music Wall

Music Wall is a student full-stack application for discovering music, saving
favourites and building shared listening walls with friends. A wall is divided
into coloured paper-like sections (for example Jazz, Summer or Focus), and each
section can contain albums or tracks from the local catalogue.

This repository contains only the final course-style version of the project.
It deliberately uses a conventional controller / DTO / entity / repository /
service structure so that the code remains understandable and presentable in a
student project.

## Current features

### Accounts and security

- registration and login with a username and password;
- BCrypt password hashing;
- JWT authentication with Spring Security;
- authenticated user derived from the JWT, never from a user ID sent by Angular;
- one immutable `username` used everywhere in the application;
- no editable username or separate display name;
- centralized validation and JSON error responses.

There are no default login credentials. Create an account on the Register page.

### Music walls

- create, open, edit and delete personal walls;
- creator automatically becomes the wall `OWNER`;
- inline wall title and description editing;
- create, rename, recolour and delete wall sections;
- add catalogue tracks or albums to a section;
- mark wall items as `TO_LISTEN` or `LISTENED`;
- remove music from a section;
- nine music-themed wallpapers plus a no-wallpaper option;
- configurable wall colour when no wallpaper is selected;
- paper-note colours, tape decoration and responsive two-column layout;
- desktop sidebar, collapsed icon rail and mobile bottom navigation.

Owners can rename and delete their walls and invite collaborators. Both owners
and accepted members can open the wall, manage its sections and music, and
change its appearance. The backend checks all wall access in the service layer.

### Friends and invitations

- search registered users by username;
- send, accept and reject friend requests;
- view and remove friends;
- invite friends to a wall;
- accept or reject wall invitations;
- view owner and member profiles from a wall;
- pending friend requests and wall invitations are shown together on the
  Friends page;
- removing a friend invalidates pending wall invitations between both users.

Only friends can be invited to a wall. This keeps the collaboration flow simple:

```text
find user -> become friends -> invite friend -> friend accepts -> wall member
```

### Catalogue and favourites

- local PostgreSQL catalogue containing artists, albums, tracks and genres;
- search, suggestions and filters for artists, albums and tracks;
- artist, album and track detail pages;
- album track lists and links between related catalogue pages;
- cover artwork URLs imported from MusicBrainz / Cover Art Archive data;
- favourite and unfavourite artists, albums and tracks;
- add catalogue albums and tracks directly to wall sections;
- back-navigation remembers whether the user came from the catalogue, profile,
  album, artist, track or wall.

Normal catalogue browsing does **not** call MusicBrainz. The application reads
the already imported catalogue from PostgreSQL, which makes the UI faster and
keeps the runtime architecture easier to explain.

### Profiles and statistics

- public profile pages addressed by immutable username;
- optional short bio;
- avatar upload from the user's computer (stored in PostgreSQL);
- favourite artists, albums and tracks;
- compact carousels and `View all` controls;
- favourite-genre distribution displayed as a donut chart;
- visibility settings for artists, albums, tracks and music taste;
- other users' profiles are read-only.

### Concerts

- optional concert search through the backend;
- uses Ticketmaster only when `TICKETMASTER_API_KEY` is configured;
- the rest of the application works without this key.

## Technologies

### Backend

- Java 17;
- Spring Boot 3.5;
- Spring Web;
- Spring Data JPA / Hibernate;
- Spring Security;
- JWT (`jjwt`);
- Bean Validation;
- PostgreSQL;
- Maven Wrapper;
- JUnit 5 and Mockito.

### Frontend

- Angular 19;
- standalone components;
- Angular Router, HttpClient and template-driven forms;
- RxJS;
- ordinary CSS with Flexbox and Grid;
- DM Serif Display for headings and Nunito for interface text.

Angular Material, Tailwind and Docker are not required. Database tables are
managed by Hibernate with `spring.jpa.hibernate.ddl-auto=update`; Flyway is not
used in this simplified version.

## Project structure

```text
new-app-final/
|-- backend/
|   |-- src/main/java/com/musicwall/
|   |   |-- controller/   REST endpoints
|   |   |-- dto/          JSON request and response objects
|   |   |-- entity/       PostgreSQL/JPA table mappings
|   |   |-- repository/   Spring Data database access
|   |   |-- service/      application logic and authorization
|   |   |-- security/     JWT filter and Spring Security configuration
|   |   `-- exception/    centralized API error handling
|   |-- src/test/         focused service tests
|   |-- .env.example
|   |-- import-catalog.cmd
|   `-- mvnw.cmd
|-- frontend/
|   `-- src/app/
|       |-- components/   pages and shared layout
|       |-- models/       TypeScript API interfaces
|       |-- services/     backend HTTP calls
|       |-- guards/       protected-route check
|       `-- interceptors/ JWT request header
|-- create-database.sql
|-- start-all.cmd
`-- start-all.ps1
```

The backend keeps entities behind DTOs. Controllers receive and return DTOs;
services load and modify entities; repositories communicate with PostgreSQL.

Frontend HTTP services are separated by purpose:

```text
AuthService       login, registration and local JWT session
MusicWallService walls, sections and wall items
CatalogService    catalogue search and detail pages
ProfileService    profiles, avatars and favourites
SocialService     friends and invitations
ConcertService    optional concert search
```

## Local setup

### Prerequisites

- Java 17;
- Node.js and npm;
- locally installed PostgreSQL;
- port `5432` available for PostgreSQL;
- ports `8080` and `4200` available for backend and frontend.

Docker is not needed for this project.

### 1. Create the PostgreSQL database

Start PostgreSQL and create the database in pgAdmin or execute:

```sql
CREATE DATABASE music_wall_final;
```

The same command is stored in `create-database.sql`.

Default connection values are:

```text
host:     localhost
port:     5432
database: music_wall_final
username: postgres
password: postgres
```

Copy the environment example before the first backend start:

```powershell
cd C:\new_music_app\new-app-final\backend
Copy-Item .env.example .env
```

Then edit `backend/.env`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/music_wall_final
DB_USERNAME=postgres
DB_PASSWORD=your_real_postgresql_password
JWT_SECRET=replace_with_a_long_random_secret_of_at_least_32_characters
TICKETMASTER_API_KEY=
MUSICBRAINZ_USER_AGENT=MusicWallStudentProject/1.0 (your-email@example.com)
```

Do not commit `backend/.env`. Hibernate creates or updates the tables when the
backend starts.

### 2. Install Angular dependencies

This is needed once after cloning:

```powershell
cd C:\new_music_app\new-app-final\frontend
npm install
```

### 3. Start the application

Make sure PostgreSQL is already running. Then double-click `start-all.cmd`, or:

```powershell
cd C:\new_music_app\new-app-final
.\start-all.ps1
```

The launcher opens backend and frontend in separate PowerShell windows:

- frontend: <http://localhost:4200>
- backend: <http://localhost:8080>

The launcher does not start PostgreSQL.

You can also start each part manually:

```powershell
# backend
cd C:\new_music_app\new-app-final\backend
.\mvnw.cmd spring-boot:run

# frontend (in another terminal)
cd C:\new_music_app\new-app-final\frontend
npm start
```

## Curated MusicBrainz import

The selected artists and exact MusicBrainz IDs are stored in:

```text
backend/src/main/resources/catalog-artists.json
```

To populate an empty catalogue, make sure PostgreSQL is running and launch:

```text
backend/import-catalog.cmd
```

The importer downloads a controlled subset of albums, recordings and genres
for the configured artists. It can take several minutes because MusicBrainz
limits request speed. Existing MusicBrainz IDs prevent duplicate catalogue
records, so an interrupted import can be run again.

The import is a maintenance command, not part of normal user search. Do not run
it while the ordinary backend is already using port `8080`; stop that backend
first, run the import, then relaunch the application.

## Important backend flows

### Registration and login

```text
Angular form
  -> AuthService (Angular)
  -> AuthController
  -> AuthService (Spring)
  -> UserRepository
  -> PostgreSQL
```

After login, Angular stores the JWT and immutable username. The interceptor adds
the token to protected HTTP requests. There is no editable `displayName` field.

### Creating and accessing a wall

```text
Angular sends wall data
  -> JwtFilter validates the token
  -> controller reads Authentication.getName()
  -> service uses that username
  -> repository reads or writes PostgreSQL
```

Angular never supplies an owner ID. The backend sets the authenticated user as
the owner when a wall is created.

`WallAccessService` contains the central rules:

```text
findOwnedWall       owner-only operations such as deleting a wall or inviting
findAccessibleWall  operations available to the owner and accepted members
```

This is real authorization. Hiding a button in Angular improves the interface,
but the backend service check is what prevents unauthorized HTTP requests.

### Catalogue model

```text
ArtistEntity
  -> AlbumEntity
       -> TrackEntity

AlbumEntity <-> GenreEntity
TrackEntity <-> GenreEntity
```

An album or track placed on a wall keeps a reference to the corresponding local
catalogue entity. Users do not type a second custom title or artist name.

## Frontend routes

```text
/login             login
/register          registration
/dashboard         overview and recent walls
/walls             complete wall list and quick wall creation
/walls/:id         wall detail, members, sections and music
/catalog            catalogue search
/catalog/:type/:id artist, album or track detail
/profile            current user's profile
/users/:username    another user's public profile
/friends            friends, friend requests and wall invitations
/concerts           optional concert search
```

The Angular application places these pages inside one authenticated layout.
Catalogue and profile `GET` endpoints are public at backend level, but the
current Angular routes are inside the authenticated application shell.

## Tests and verification

Run backend tests:

```powershell
cd C:\new_music_app\new-app-final\backend
.\mvnw.cmd test
```

The current suite contains focused tests for catalogue logic, friendships,
invitations, MusicBrainz import, wall sections and profiles.

Build the Angular application:

```powershell
cd C:\new_music_app\new-app-final\frontend
npm run build
```

At the time of this README update:

- all 21 backend tests pass;
- the Angular production build succeeds;
- Angular reports a non-blocking initial-bundle budget warning (about 560 kB
  versus the configured 500 kB budget).

## Current limitations

- PostgreSQL must be started separately;
- catalogue contents are limited to the curated MusicBrainz import;
- MusicBrainz availability only affects the import command, not normal browsing;
- concert results require a valid Ticketmaster API key;
- there is no email verification, password reset or administrator interface;
- wall invitations are restricted to accepted friends;
- Hibernate updates schemas but does not provide versioned migrations;
- this is a course project, not a production deployment configuration.
