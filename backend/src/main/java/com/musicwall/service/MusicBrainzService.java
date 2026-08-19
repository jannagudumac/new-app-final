package com.musicwall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicwall.dto.AlbumDTO;
import com.musicwall.dto.ArtistDTO;
import com.musicwall.dto.CatalogSearchDTO;
import com.musicwall.dto.CatalogImportResultDTO;
import com.musicwall.dto.TrackDTO;
import com.musicwall.entity.AlbumEntity;
import com.musicwall.entity.ArtistEntity;
import com.musicwall.entity.GenreEntity;
import com.musicwall.entity.TrackEntity;
import com.musicwall.exception.BusinessException;
import com.musicwall.repository.AlbumRepository;
import com.musicwall.repository.ArtistRepository;
import com.musicwall.repository.GenreRepository;
import com.musicwall.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MusicBrainzService {

    private static final long REQUEST_INTERVAL_MILLISECONDS = 1100L;
    private static final long CACHE_LIFETIME_SECONDS = 300L;
    private static final int MAX_COMPOSER_RECORDINGS = 10;

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final GenreRepository genreRepository;
    private final RestClient restClient;
    private final String userAgent;
    private final Map<String, CachedSearch> cache = new LinkedHashMap<>();
    private long lastRequestTime;

    public MusicBrainzService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            TrackRepository trackRepository,
            GenreRepository genreRepository,
            @Value("${musicbrainz.base-url}") String baseUrl,
            @Value("${musicbrainz.user-agent}") String userAgent
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.trackRepository = trackRepository;
        this.genreRepository = genreRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.userAgent = userAgent;
    }

    /**
     * Imports a small, controlled part of one artist's catalogue.
     * Search results never call this method automatically.
     */
    public CatalogImportResultDTO importArtistCatalogue(
            String musicBrainzId,
            String displayName,
            int maxAlbums
    ) {
        var existingArtist = artistRepository.findByMusicBrainzId(musicBrainzId);
        if (existingArtist.isPresent()) {
            long albumCount = albumRepository.countByArtistId(existingArtist.get().getId());
            long trackCount = trackRepository.countByArtistId(existingArtist.get().getId());
            boolean importedWithContent = existingArtist.get().isCatalogImported()
                    && (albumCount > 0 || trackCount > 0);
            if (importedWithContent || albumCount >= maxAlbums) {
                if (!existingArtist.get().isCatalogImported()) {
                    existingArtist.get().setCatalogImported(true);
                    artistRepository.save(existingArtist.get());
                }
                return new CatalogImportResultDTO();
            }
        }

        JsonNode artistNode = requestLookupWithRetry(
                "/ws/2/artist/" + musicBrainzId,
                "genres"
        );
        ArtistEntity artist = saveArtist(text(artistNode, "id"), displayName);

        CatalogImportResultDTO result = new CatalogImportResultDTO();
        result.addArtist();

        JsonNode response = requestBrowseReleaseGroups(musicBrainzId);
        List<JsonNode> groups = selectAlbumGroups(response, maxAlbums);
        for (JsonNode groupSummary : groups) {
            String groupId = text(groupSummary, "id");
            if (groupId == null) continue;

            try {
                JsonNode group = requestLookupWithRetry(
                        "/ws/2/release-group/" + groupId,
                        "artist-credits+releases+genres"
                );
                JsonNode release = findReleaseWithTracks(group);
                if (release == null) {
                    result.addError(displayName + ": no track list for " + text(group, "title"));
                    continue;
                }

                AlbumEntity album = saveAlbum(
                        groupId,
                        text(group, "title"),
                        text(release, "id"),
                        parseYear(text(group, "first-release-date")),
                        artist
                );
                if (album == null) continue;

                Set<GenreEntity> genres = saveGenres(group.path("genres"));
                album.setGenres(new LinkedHashSet<>(genres));
                albumRepository.save(album);

                int importedTracks = saveReleaseTracks(release, artist, album, genres);
                result.addAlbums(1);
                result.addTracks(importedTracks);
            } catch (BusinessException exception) {
                result.addError(displayName + ": " + text(groupSummary, "title") + " was skipped");
            }
        }

        // A composer is normally related to works rather than credited as the
        // performer of an album. In that case release-group browsing is empty,
        // so import a small number of real recordings linked to their works.
        if (result.getAlbums() == 0 && result.getTracks() == 0) {
            result.addTracks(importComposerRecordings(musicBrainzId, artist));
        }

        boolean hasContent = albumRepository.countByArtistId(artist.getId()) > 0
                || trackRepository.countByArtistId(artist.getId()) > 0;
        artist.setCatalogImported(hasContent);
        artistRepository.save(artist);
        if (!hasContent) {
            result.addError(displayName + ": MusicBrainz returned no albums or recordings");
        }
        return result;
    }

    private int importComposerRecordings(String artistMusicBrainzId, ArtistEntity artist) {
        String query = "arid:" + artistMusicBrainzId + " AND recording_count:[1 TO *]";
        JsonNode workResponse = safeSearch("/ws/2/work", query, MAX_COMPOSER_RECORDINGS);
        if (workResponse == null) return 0;

        int imported = 0;
        for (JsonNode work : array(workResponse, "works")) {
            String workId = text(work, "id");
            if (workId == null) continue;

            JsonNode recordingResponse = requestBrowseRecordings(workId);
            JsonNode recording = first(recordingResponse, "recordings");
            if (recording == null) continue;

            String recordingId = text(recording, "id");
            String title = text(recording, "title");
            if (recordingId == null || title == null) continue;

            saveTrack(recordingId, title, recording.path("length"), artist, null);
            imported++;
        }
        return imported;
    }

    List<JsonNode> selectAlbumGroups(JsonNode response, int maxAlbums) {
        List<JsonNode> groups = new ArrayList<>();
        for (JsonNode group : array(response, "release-groups")) {
            String primaryType = text(group, "primary-type");
            if (!"Album".equalsIgnoreCase(primaryType) && !"EP".equalsIgnoreCase(primaryType)) {
                continue;
            }
            if (hasExcludedSecondaryType(group)) continue;
            groups.add(group);
        }
        groups.sort(Comparator
                .comparing((JsonNode node) -> text(node, "first-release-date"),
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(node -> text(node, "title"), Comparator.nullsLast(String::compareToIgnoreCase)));
        return groups.stream().limit(Math.max(0, maxAlbums)).toList();
    }

    private boolean hasExcludedSecondaryType(JsonNode group) {
        JsonNode types = group.path("secondary-types");
        if (!types.isArray()) return false;
        for (JsonNode type : types) {
            String value = type.asText("");
            if ("Compilation".equalsIgnoreCase(value)
                    || "DJ-mix".equalsIgnoreCase(value)
                    || "Mixtape/Street".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findReleaseWithTracks(JsonNode group) {
        List<JsonNode> releases = new ArrayList<>();
        for (JsonNode release : array(group, "releases")) releases.add(release);
        releases.sort(Comparator
                .comparing((JsonNode node) -> !"Official".equalsIgnoreCase(text(node, "status")))
                .thenComparing(node -> text(node, "date"), Comparator.nullsLast(String::compareTo)));

        for (JsonNode releaseSummary : releases.stream().limit(5).toList()) {
            String releaseId = text(releaseSummary, "id");
            if (releaseId == null) continue;
            JsonNode release = requestLookupWithRetry(
                    "/ws/2/release/" + releaseId,
                    "recordings+artist-credits+release-groups"
            );
            if (hasTracks(release)) return release;
        }
        return null;
    }

    private boolean hasTracks(JsonNode release) {
        JsonNode media = release.path("media");
        if (!media.isArray()) return false;
        for (JsonNode medium : media) {
            if (medium.path("tracks").isArray() && !medium.path("tracks").isEmpty()) return true;
        }
        return false;
    }

    private int saveReleaseTracks(
            JsonNode release,
            ArtistEntity artist,
            AlbumEntity album,
            Set<GenreEntity> genres
    ) {
        int count = 0;
        for (JsonNode medium : array(release, "media")) {
            for (JsonNode trackNode : array(medium, "tracks")) {
                JsonNode recording = trackNode.path("recording");
                String recordingId = text(recording, "id");
                String title = text(trackNode, "title");
                if (title == null) title = text(recording, "title");
                if (recordingId == null || title == null) continue;

                JsonNode length = trackNode.path("length");
                if (!length.canConvertToInt()) length = recording.path("length");
                TrackEntity track = saveTrack(recordingId, title, length, artist, album);
                track.setGenres(new LinkedHashSet<>(genres));
                trackRepository.save(track);
                count++;
            }
        }
        return count;
    }

    private Set<GenreEntity> saveGenres(JsonNode genreNodes) {
        Set<GenreEntity> genres = new LinkedHashSet<>();
        if (!genreNodes.isArray()) return genres;

        List<JsonNode> sorted = new ArrayList<>();
        genreNodes.forEach(sorted::add);
        sorted.sort(Comparator.comparingInt(node -> -node.path("count").asInt(0)));
        for (JsonNode node : sorted.stream().limit(3).toList()) {
            String normalizedName = normalizeGenre(text(node, "name"));
            if (normalizedName == null) continue;
            GenreEntity genre = genreRepository.findFirstByNameIgnoreCase(normalizedName)
                    .orElseGet(GenreEntity::new);
            genre.setName(normalizedName);
            genres.add(genreRepository.save(genre));
        }
        return genres;
    }

    String normalizeGenre(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toLowerCase()) {
            case "classical", "modern classical", "romanticism", "neoclassicism" -> "Classical";
            case "progressive rock", "prog rock", "art rock" -> "Progressive Rock";
            case "hard rock" -> "Hard Rock";
            case "heavy metal", "metal" -> "Metal";
            case "progressive metal" -> "Progressive Metal";
            case "electronic", "electronica" -> "Electronic";
            case "synth-pop", "synthpop" -> "Synth-pop";
            case "folk rock" -> "Folk Rock";
            case "indie rock", "alternative rock" -> "Alternative Rock";
            case "jazz" -> "Jazz";
            case "funk" -> "Funk";
            case "folk" -> "Folk";
            case "celtic" -> "Celtic";
            case "pop", "italian pop" -> "Pop";
            case "ambient" -> "Ambient";
            case "world", "world music" -> "World";
            case "african" -> "African";
            default -> Character.toUpperCase(value.trim().charAt(0)) + value.trim().substring(1).toLowerCase();
        };
    }

    /** Search is read-only: nothing from these responses is persisted. */
    public synchronized CatalogSearchDTO search(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.length() < 2) {
            return emptyResult();
        }

        String cacheKey = cleanQuery.toLowerCase();
        CachedSearch cachedSearch = cache.get(cacheKey);
        if (cachedSearch != null && !cachedSearch.isExpired()) {
            return cachedSearch.result;
        }

        JsonNode artistResponse = safeSearch("/ws/2/artist", fieldQuery("artist", cleanQuery), 10);
        JsonNode albumResponse = safeSearch(
                "/ws/2/release-group",
                fieldQuery("artist", cleanQuery) + "^3 OR " + fieldQuery("releasegroup", cleanQuery),
                10
        );
        JsonNode trackResponse = safeSearch(
                "/ws/2/recording",
                fieldQuery("artist", cleanQuery) + "^3 OR " + fieldQuery("recording", cleanQuery),
                15
        );
        if (artistResponse == null && albumResponse == null && trackResponse == null) {
            throw unavailable();
        }

        CatalogSearchDTO result = new CatalogSearchDTO();
        result.setArtists(artistResponse == null ? List.of() : parseArtistSearch(artistResponse));
        result.setAlbums(albumResponse == null ? List.of() : parseAlbumSearch(albumResponse));
        result.setTracks(trackResponse == null ? List.of() : parseTrackSearch(trackResponse));
        result.setGenres(List.of());
        List<String> warnings = new ArrayList<>();
        if (artistResponse == null) warnings.add("Artist results could not be loaded.");
        if (albumResponse == null) warnings.add("Album results could not be loaded.");
        if (trackResponse == null) warnings.add("Track results could not be loaded.");
        result.setWarnings(warnings);

        if (warnings.isEmpty()) {
            cache.put(cacheKey, new CachedSearch(result));
        }
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return result;
    }

    List<ArtistDTO> parseArtistSearch(JsonNode response) {
        List<ArtistDTO> results = new ArrayList<>();
        for (JsonNode node : array(response, "artists")) {
            String id = text(node, "id");
            String name = text(node, "name");
            if (id == null || name == null) continue;
            ArtistDTO dto = new ArtistDTO();
            dto.setMusicBrainzId(id);
            dto.setName(name);
            results.add(dto);
        }
        return results;
    }

    List<AlbumDTO> parseAlbumSearch(JsonNode response) {
        List<AlbumDTO> results = new ArrayList<>();
        for (JsonNode node : array(response, "release-groups")) {
            JsonNode artistNode = firstArtist(node);
            String id = text(node, "id");
            String title = text(node, "title");
            String artistName = text(artistNode, "name");
            if (id == null || title == null || artistName == null) continue;
            AlbumDTO dto = new AlbumDTO();
            dto.setMusicBrainzId(id);
            dto.setTitle(title);
            dto.setArtistName(artistName);
            dto.setArtistMusicBrainzId(text(artistNode, "id"));
            dto.setReleaseYear(parseYear(text(node, "first-release-date")));
            dto.setCoverUrl("https://coverartarchive.org/release-group/" + id + "/front-250");
            dto.setGenres(List.of());
            results.add(dto);
        }
        return results;
    }

    List<TrackDTO> parseTrackSearch(JsonNode response) {
        List<TrackDTO> results = new ArrayList<>();
        for (JsonNode node : array(response, "recordings")) {
            JsonNode artistNode = firstArtist(node);
            String id = text(node, "id");
            String title = text(node, "title");
            String artistName = text(artistNode, "name");
            if (id == null || title == null || artistName == null) continue;
            TrackDTO dto = new TrackDTO();
            dto.setMusicBrainzId(id);
            dto.setTitle(title);
            dto.setArtistName(artistName);
            dto.setArtistMusicBrainzId(text(artistNode, "id"));
            if (node.path("length").canConvertToInt()) {
                dto.setDurationSeconds(node.path("length").asInt() / 1000);
            }
            JsonNode release = firstAlbumRelease(node);
            if (release != null) {
                dto.setAlbumTitle(text(release.path("release-group"), "title"));
                dto.setAlbumMusicBrainzId(text(release.path("release-group"), "id"));
                String releaseId = text(release, "id");
                if (releaseId != null) {
                    dto.setAlbumCoverUrl("https://coverartarchive.org/release/" + releaseId + "/front-250");
                }
            }
            dto.setGenres(List.of());
            results.add(dto);
        }
        return results;
    }

    @Transactional
    public ArtistDTO importArtist(String musicBrainzId) {
        JsonNode node = requestLookup("/ws/2/artist/" + musicBrainzId, null);
        ArtistEntity artist = saveArtist(text(node, "id"), text(node, "name"));
        return convertArtist(artist);
    }

    @Transactional
    public AlbumDTO importAlbum(String musicBrainzId) {
        JsonNode node = requestLookup(
                "/ws/2/release-group/" + musicBrainzId,
                "artist-credits+releases"
        );
        JsonNode artistNode = firstArtist(node);
        ArtistEntity artist = saveArtist(text(artistNode, "id"), text(artistNode, "name"));
        AlbumEntity album = saveAlbum(
                text(node, "id"), text(node, "title"),
                firstReleaseId(node), parseYear(text(node, "first-release-date")), artist
        );
        return convertAlbum(album);
    }

    @Transactional
    public TrackDTO importTrack(String musicBrainzId) {
        JsonNode node = requestLookup(
                "/ws/2/recording/" + musicBrainzId,
                "artist-credits+releases+release-groups"
        );
        JsonNode artistNode = firstArtist(node);
        ArtistEntity artist = saveArtist(text(artistNode, "id"), text(artistNode, "name"));
        JsonNode release = firstAlbumRelease(node);
        AlbumEntity album = null;
        if (release != null) {
            JsonNode group = release.path("release-group");
            album = saveAlbum(
                    text(group, "id"), text(group, "title"), text(release, "id"),
                    parseYear(text(release, "date")), artist
            );
        }
        TrackEntity track = saveTrack(
                text(node, "id"), text(node, "title"), node.path("length"), artist, album
        );
        return convertTrack(track);
    }

    public ArtistDTO getArtist(String musicBrainzId) {
        JsonNode node = requestLookup("/ws/2/artist/" + musicBrainzId, null);
        ArtistDTO dto = new ArtistDTO();
        dto.setMusicBrainzId(text(node, "id"));
        dto.setName(text(node, "name"));
        artistRepository.findByMusicBrainzId(musicBrainzId).ifPresent(local -> dto.setId(local.getId()));
        return dto;
    }

    public AlbumDTO getAlbum(String musicBrainzId) {
        JsonNode node = requestLookup("/ws/2/release-group/" + musicBrainzId, "artist-credits+releases");
        JsonNode artist = firstArtist(node);
        AlbumDTO dto = new AlbumDTO();
        dto.setMusicBrainzId(text(node, "id"));
        dto.setTitle(text(node, "title"));
        dto.setArtistMusicBrainzId(text(artist, "id"));
        dto.setArtistName(text(artist, "name"));
        dto.setReleaseYear(parseYear(text(node, "first-release-date")));
        dto.setCoverUrl("https://coverartarchive.org/release-group/" + musicBrainzId + "/front-250");
        dto.setGenres(List.of());
        albumRepository.findByMusicBrainzId(musicBrainzId).ifPresent(local -> {
            dto.setId(local.getId());
            dto.setArtistId(local.getArtist().getId());
        });
        return dto;
    }

    public TrackDTO getTrack(String musicBrainzId) {
        JsonNode node = requestLookup("/ws/2/recording/" + musicBrainzId,
                "artist-credits+releases+release-groups");
        JsonNode artist = firstArtist(node);
        TrackDTO dto = new TrackDTO();
        dto.setMusicBrainzId(text(node, "id"));
        dto.setTitle(text(node, "title"));
        dto.setArtistMusicBrainzId(text(artist, "id"));
        dto.setArtistName(text(artist, "name"));
        if (node.path("length").canConvertToInt()) dto.setDurationSeconds(node.path("length").asInt() / 1000);
        JsonNode release = firstAlbumRelease(node);
        if (release != null) {
            JsonNode group = release.path("release-group");
            dto.setAlbumMusicBrainzId(text(group, "id"));
            dto.setAlbumTitle(text(group, "title"));
            String releaseId = text(release, "id");
            if (releaseId != null) dto.setAlbumCoverUrl("https://coverartarchive.org/release/" + releaseId + "/front-250");
        }
        dto.setGenres(List.of());
        trackRepository.findByMusicBrainzId(musicBrainzId).ifPresent(local -> {
            dto.setId(local.getId());
            dto.setArtistId(local.getArtist().getId());
            if (local.getAlbum() != null) dto.setAlbumId(local.getAlbum().getId());
        });
        return dto;
    }

    private synchronized JsonNode requestSearch(String path, String query, int limit) {
        waitForRateLimit();
        try {
            JsonNode response = restClient.get()
                    .uri(builder -> builder.path(path)
                            .queryParam("query", query)
                            .queryParam("fmt", "json")
                            .queryParam("limit", limit)
                            .build())
                    .header("User-Agent", userAgent).retrieve().body(JsonNode.class);
            lastRequestTime = System.currentTimeMillis();
            return response;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private JsonNode safeSearch(String path, String query, int limit) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return requestSearch(path, query, limit);
            } catch (BusinessException exception) {
                // MusicBrainz occasionally returns a temporary 503. The next
                // request still passes through the shared rate limiter.
            }
        }
        return null;
    }

    private String fieldQuery(String field, String query) {
        String escaped = query.replace("\\", "\\\\").replace("\"", "\\\"");
        return field + ":\"" + escaped + "\"";
    }

    private synchronized JsonNode requestLookup(String path, String inc) {
        waitForRateLimit();
        try {
            JsonNode response = restClient.get()
                    .uri(builder -> {
                        builder.path(path).queryParam("fmt", "json");
                        if (inc != null) builder.queryParam("inc", inc);
                        return builder.build();
                    })
                    .header("User-Agent", userAgent).retrieve().body(JsonNode.class);
            return response;
        } catch (RestClientException exception) {
            throw unavailable();
        } finally {
            lastRequestTime = System.currentTimeMillis();
        }
    }

    private JsonNode requestLookupWithRetry(String path, String inc) {
        BusinessException lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return requestLookup(path, inc);
            } catch (BusinessException exception) {
                lastException = exception;
            }
        }
        throw lastException == null ? unavailable() : lastException;
    }

    private synchronized JsonNode requestBrowseReleaseGroups(String artistMusicBrainzId) {
        waitForRateLimit();
        try {
            return restClient.get()
                    .uri(builder -> builder.path("/ws/2/release-group")
                            .queryParam("artist", artistMusicBrainzId)
                            .queryParam("fmt", "json")
                            .queryParam("limit", 100)
                            .build())
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw unavailable();
        } finally {
            lastRequestTime = System.currentTimeMillis();
        }
    }

    private synchronized JsonNode requestBrowseRecordings(String workMusicBrainzId) {
        waitForRateLimit();
        try {
            return restClient.get()
                    .uri(builder -> builder.path("/ws/2/recording")
                            .queryParam("work", workMusicBrainzId)
                            .queryParam("fmt", "json")
                            .queryParam("limit", 1)
                            .build())
                    .header("User-Agent", userAgent)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            return null;
        } finally {
            lastRequestTime = System.currentTimeMillis();
        }
    }

    private ArtistEntity saveArtist(String musicBrainzId, String name) {
        if (musicBrainzId == null || name == null) throw unavailable();
        ArtistEntity artist = artistRepository.findByMusicBrainzId(musicBrainzId)
                .orElseGet(() -> artistRepository.findFirstByNameIgnoreCase(name)
                        .orElseGet(ArtistEntity::new));
        artist.setMusicBrainzId(musicBrainzId);
        artist.setName(name);
        return artistRepository.save(artist);
    }

    private AlbumEntity saveAlbum(String musicBrainzId, String title, String releaseId,
                                  Integer year, ArtistEntity artist) {
        if (musicBrainzId == null || title == null) return null;
        AlbumEntity album = albumRepository.findByMusicBrainzId(musicBrainzId)
                .orElseGet(() -> albumRepository
                        .findFirstByArtistIdAndTitleIgnoreCase(artist.getId(), title)
                        .orElseGet(AlbumEntity::new));
        album.setMusicBrainzId(musicBrainzId);
        album.setMusicBrainzReleaseId(releaseId);
        album.setTitle(title);
        album.setArtist(artist);
        album.setReleaseYear(year);
        album.setCoverUrl("https://coverartarchive.org/release-group/" + musicBrainzId + "/front-250");
        return albumRepository.save(album);
    }

    private TrackEntity saveTrack(String musicBrainzId, String title, JsonNode length,
                                  ArtistEntity artist, AlbumEntity album) {
        if (musicBrainzId == null || title == null) throw unavailable();
        TrackEntity track = trackRepository.findByMusicBrainzId(musicBrainzId)
                .orElseGet(() -> trackRepository
                        .findFirstByArtistIdAndTitleIgnoreCase(artist.getId(), title)
                        .orElseGet(TrackEntity::new));
        track.setMusicBrainzId(musicBrainzId);
        track.setTitle(title);
        track.setArtist(artist);
        if (track.getAlbum() == null) track.setAlbum(album);
        if (length.canConvertToInt()) track.setDurationSeconds(length.asInt() / 1000);
        return trackRepository.save(track);
    }

    private ArtistDTO convertArtist(ArtistEntity artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setMusicBrainzId(artist.getMusicBrainzId());
        dto.setName(artist.getName());
        return dto;
    }

    private AlbumDTO convertAlbum(AlbumEntity album) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setMusicBrainzId(album.getMusicBrainzId());
        dto.setTitle(album.getTitle());
        dto.setReleaseYear(album.getReleaseYear());
        dto.setCoverUrl(album.getCoverUrl());
        dto.setArtistId(album.getArtist().getId());
        dto.setArtistMusicBrainzId(album.getArtist().getMusicBrainzId());
        dto.setArtistName(album.getArtist().getName());
        dto.setGenres(List.of());
        return dto;
    }

    private TrackDTO convertTrack(TrackEntity track) {
        TrackDTO dto = new TrackDTO();
        dto.setId(track.getId());
        dto.setMusicBrainzId(track.getMusicBrainzId());
        dto.setTitle(track.getTitle());
        dto.setDurationSeconds(track.getDurationSeconds());
        dto.setArtistId(track.getArtist().getId());
        dto.setArtistMusicBrainzId(track.getArtist().getMusicBrainzId());
        dto.setArtistName(track.getArtist().getName());
        if (track.getAlbum() != null) {
            dto.setAlbumId(track.getAlbum().getId());
            dto.setAlbumMusicBrainzId(track.getAlbum().getMusicBrainzId());
            dto.setAlbumTitle(track.getAlbum().getTitle());
            dto.setAlbumCoverUrl(track.getAlbum().getCoverUrl());
        }
        dto.setGenres(List.of());
        return dto;
    }

    private JsonNode firstArtist(JsonNode node) {
        JsonNode credits = node == null ? null : node.path("artist-credit");
        if (credits == null || !credits.isArray() || credits.isEmpty()) return null;
        return credits.get(0).path("artist");
    }

    private JsonNode firstAlbumRelease(JsonNode node) {
        JsonNode releases = node == null ? null : node.path("releases");
        if (releases == null || !releases.isArray()) return null;
        for (JsonNode release : releases) {
            String type = text(release.path("release-group"), "primary-type");
            if ("Album".equalsIgnoreCase(type) || "EP".equalsIgnoreCase(type)) return release;
        }
        return releases.isEmpty() ? null : releases.get(0);
    }

    private String firstReleaseId(JsonNode releaseGroup) {
        JsonNode releases = releaseGroup.path("releases");
        return releases.isArray() && !releases.isEmpty() ? text(releases.get(0), "id") : null;
    }

    private Iterable<JsonNode> array(JsonNode response, String field) {
        JsonNode value = response == null ? null : response.path(field);
        return value != null && value.isArray() ? value : List.of();
    }

    private JsonNode first(JsonNode response, String field) {
        JsonNode value = response == null ? null : response.path(field);
        return value != null && value.isArray() && !value.isEmpty() ? value.get(0) : null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) return null;
        String value = node.path(field).asText().trim();
        return value.isEmpty() ? null : value;
    }

    private Integer parseYear(String date) {
        if (date == null || date.length() < 4) return null;
        try { return Integer.valueOf(date.substring(0, 4)); }
        catch (NumberFormatException exception) { return null; }
    }

    private void waitForRateLimit() {
        long wait = REQUEST_INTERVAL_MILLISECONDS - (System.currentTimeMillis() - lastRequestTime);
        if (wait <= 0) return;
        try { Thread.sleep(wait); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("MusicBrainz search was interrupted");
        }
    }

    private BusinessException unavailable() {
        return new BusinessException("MusicBrainz is temporarily unavailable. Please try again.");
    }

    private CatalogSearchDTO emptyResult() {
        CatalogSearchDTO result = new CatalogSearchDTO();
        result.setArtists(List.of());
        result.setAlbums(List.of());
        result.setTracks(List.of());
        result.setGenres(List.of());
        return result;
    }

    private static class CachedSearch {
        private final CatalogSearchDTO result;
        private final Instant createdAt = Instant.now();

        private CachedSearch(CatalogSearchDTO result) { this.result = result; }

        private boolean isExpired() {
            return createdAt.plusSeconds(CACHE_LIFETIME_SECONDS).isBefore(Instant.now());
        }
    }
}
