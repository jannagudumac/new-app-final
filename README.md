# Music Wall - simple course-style version

This project is a simpler reconstruction of the first Music Wall milestone.
It is independent from `new-app`.

## Current features

- register with a username and password;
- login and receive a JWT;
- create a private music wall;
- the authenticated user automatically becomes its owner;
- display the authenticated user's walls on the dashboard;
- prevent another user from opening or changing a wall they do not own;
- open a wall detail page;
- edit and delete a wall;
- create, rename and delete custom sections such as Bebop or Baroque;
- add tracks and albums to a section;
- mark each item as `TO_LISTEN` or `LISTENED`;
- edit and delete tracks and albums;
- choose No wallpaper or one of eight imported music-themed patterns;
- choose the base color of each wall with a color picker;
- display sections as taped paper notes on the selected wall background;
- responsive authenticated layout with a collapsible desktop sidebar and a
  thin translucent bottom navigation ribbon on phones;
- dashboard overview and a separate My Walls page.
- local catalogue search across imported artists, albums, tracks and genres;
- public artist, album and track detail pages;
- controlled one-time catalogue import from MusicBrainz;
- add catalogue tracks and albums to private wall sections;
- favourite tracks and show them on public user profiles;
- calculate genre statistics from a user's favourite tracks;
- invite registered users to walls and accept or reject invitations;
- OWNER and MEMBER wall roles with backend membership checks;
- optional Ticketmaster concert search through the Spring Boot backend.

Legacy manually entered wall cards remain readable, while new cards can point
to global catalogue records. Concert search is the only optional feature: it
requires a Ticketmaster developer key in `backend/.env`.

## Project structure

Backend:

```text
controller  receives HTTP requests
dto         data exchanged with Angular
entity      Java classes mapped to PostgreSQL tables
repository  communicates with the database
service     contains the application logic
security    JWT and Spring Security configuration
exception   converts exceptions into simple HTTP error messages
```

Frontend:

```text
components   pages displayed to the user
             layout and sidebar build the authenticated application shell
models       TypeScript interfaces
services     HTTP calls to the backend
guards       protects the dashboard route
interceptors adds the JWT to HTTP requests
```

## Technologies

- Java 17 and Spring Boot;
- Spring Data JPA;
- Spring Security and JWT;
- PostgreSQL installed locally;
- Angular 19 standalone components;
- ordinary component CSS with native Flexbox/Grid layouts;
- Hibernate `ddl-auto=update` (no Flyway);
- no Docker.

Angular Material and Tailwind are not used. The sidebar is an ordinary Angular
component and every visual rule is written in its component CSS file.

The wall detail page opens its backgrounds from a compact Customize popover
with visual previews. Its circular rainbow button uses the
browser's native color selector while avoiding the large rectangular color
input in the page layout. The selected color fills the complete page. Wallpaper
Eight seamless patterns from the supplied `backgrounds` packages are stored as
web-optimized JPEG files in `frontend/src/assets/wallpapers`. Their names
in the switcher are Midnight Cassettes, Mint Tape Grid, Acoustic Pastels,
School Jam, Carnival Night, Music Doodles, Retro Hi-Fi and Vintage Sound.

## 1. Prepare PostgreSQL

Start your locally installed PostgreSQL server.

Create the empty database with pgAdmin, or execute:

```sql
CREATE DATABASE music_wall_final;
```

The same command is available in `create-database.sql`.

The default backend configuration expects:

```text
host:     localhost
port:     5432
database: music_wall_final
username: postgres
password: postgres
```

If your PostgreSQL password is different, copy `backend/.env.example` to
`backend/.env` and replace `DB_PASSWORD` with your real password. Do not add
spaces around `=`.

Hibernate will create the wall and catalogue tables plus favourites,
memberships and invitations when the backend starts. Restart the backend after
pulling changes so Hibernate can update the schema.

## 2. Install frontend dependencies

This is needed only the first time:

```powershell
cd C:\new_music_app\new-app-final\frontend
npm install
```

## 3. Import the selected music catalogue

The artist list is stored in:

```text
backend/src/main/resources/catalog-artists.json
```

Each entry contains a readable name, an exact MusicBrainz ID and the maximum
number of albums to import. Once PostgreSQL is running, double-click:

```text
backend/import-catalog.cmd
```

The command imports artists, release groups as albums, one official release
and its recordings as tracks. It can take several minutes because MusicBrainz
limits request speed. If MusicBrainz is temporarily unavailable, stop with
Ctrl+C and run the command again later. Existing MusicBrainz IDs and the
`catalog_imported` flag prevent duplicate data and let the import continue.

Normal application searches never call MusicBrainz. After import they use the
ordinary local PostgreSQL repositories.

## 4. Start everything

Make sure PostgreSQL is running, then double-click `start-all.cmd`, or run:

```powershell
cd C:\new_music_app\new-app-final
.\start-all.ps1
```

The launcher opens two visible terminals:

- backend: `http://localhost:8080`;
- frontend: `http://localhost:4200`.

PostgreSQL is not started by this script. It uses the PostgreSQL service
already installed on your computer.

## Backend flow in simple terms

Registration:

```text
RegisterComponent
  -> AuthService (Angular)
  -> AuthController
  -> AuthService (Spring)
  -> UserRepository
  -> PostgreSQL
```

Creating a wall:

```text
DashboardComponent sends only name and description
  -> JwtFilter identifies the user from the token
  -> MusicWallController reads Authentication.getName()
  -> MusicWallService loads that user and sets wall.owner
  -> MusicWallRepository saves the wall
```

Angular never sends an owner ID. The backend decides who the owner is from the
authenticated username.

Opening or changing wall content follows the same security rule:

```text
Angular sends the wall ID and the content to change
  -> JwtFilter identifies the logged-in username
  -> Controller passes that username to the service
  -> Service loads a wall belonging to that username
  -> only then does it read or change sections and music items
```

This check lives in the service layer, not only in the page. Hiding a button in
Angular is useful for the interface, but it is not security because an HTTP
request can be sent without using Angular.

The data hierarchy is deliberately simple:

```text
MusicWallEntity
  -> MusicSectionEntity (Bebop, Baroque, ...)
       -> MusicItemEntity (track or album, to listen or listened)
```

Entities represent database tables. DTO classes represent JSON exchanged with
Angular, so JPA entities are not exposed directly by the controllers.

Catalogue search follows the same layers:

```text
CatalogComponent
  -> CatalogService (Angular HTTP calls)
  -> CatalogController
  -> CatalogService (Spring application logic)
  -> Artist/Album/Track/Genre repositories
  -> PostgreSQL
```

Albums and tracks reference an artist. Their many-to-many genre relationships
use the `album_genre` and `track_genre` join tables. A join table is needed
because one track can have several genres and one genre can describe several
tracks. Public `GET /api/catalog/**` requests do not require a JWT. MusicBrainz
is used only by the separate local import command, not by catalogue searches.

## Frontend pages

```text
/login       public login form
/register    public registration form
/catalog     search in the selected local catalogue
/catalog/:type/:id public artist, album or track details
/profile     protected personal favourites and favourite-genre statistics
/users/:username public profile
/invitations protected pending wall invitations
/concerts    protected optional Ticketmaster concert search
/dashboard   protected overview, wall count and recent walls
/walls       protected wall creation and complete wall list
/walls/:id   protected wall detail with sections, tracks and albums
```

`LayoutComponent` owns one simple boolean:

```text
sidebarCollapsed controls the narrow desktop icon rail
```

`SidebarComponent` contains navigation and logout. On desktop its burger button
switches between the full menu and the narrow icon rail. On a small screen CSS
turns the same component into a fixed bottom ribbon with Dashboard, My walls
and Logout buttons. This keeps the Angular logic simple: there is no separate
mobile menu state or drawer overlay.
