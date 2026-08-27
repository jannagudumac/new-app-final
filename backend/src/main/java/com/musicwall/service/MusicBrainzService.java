package com.musicwall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicwall.dto.CatalogImportResultDTO;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MusicBrainzService {

    private static final long REQUEST_INTERVAL_MILLISECONDS = 1100L;
    private static final int MAX_COMPOSER_RECORDINGS = 10;

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final GenreRepository genreRepository;
    private final RestClient restClient;
    private final String userAgent;
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
            String artistName,
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
        ArtistEntity artist = saveArtist(text(artistNode, "id"), artistName);

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
                    result.addError(artistName + ": no track list for " + text(group, "title"));
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
                result.addError(artistName + ": " + text(groupSummary, "title") + " was skipped");
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
            result.addError(artistName + ": MusicBrainz returned no albums or recordings");
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

}
