package com.musicwall.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicwall.dto.CatalogSearchDTO;
import com.musicwall.repository.AlbumRepository;
import com.musicwall.repository.ArtistRepository;
import com.musicwall.repository.GenreRepository;
import com.musicwall.repository.TrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MusicBrainzServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private GenreRepository genreRepository;

    private MusicBrainzService musicBrainzService;

    @BeforeEach
    void setUp() {
        musicBrainzService = new MusicBrainzService(
                artistRepository,
                albumRepository,
                trackRepository,
                genreRepository,
                "https://musicbrainz.org",
                "MusicWallTest/1.0 (test@example.com)"
        );
    }

    @Test
    void parsesRecordingSearchWithoutSavingAnything() throws Exception {
        String json = """
                {
                  "recordings": [{
                    "id": "recording-mbid",
                    "title": "Example Track",
                    "length": 183000,
                    "artist-credit": [{
                      "artist": {"id": "artist-mbid", "name": "Example Artist"}
                    }],
                    "releases": [{
                      "id": "release-mbid",
                      "title": "Example Album",
                      "date": "2024-05-01",
                      "release-group": {
                        "id": "group-mbid",
                        "title": "Example Album",
                        "primary-type": "Album"
                      }
                    }]
                  }]
                }
                """;
        JsonNode response = new ObjectMapper().readTree(json);

        var tracks = musicBrainzService.parseTrackSearch(response);

        assertEquals(1, tracks.size());
        assertEquals("recording-mbid", tracks.get(0).getMusicBrainzId());
        assertEquals(183, tracks.get(0).getDurationSeconds());
        assertEquals("Example Album", tracks.get(0).getAlbumTitle());
        verify(artistRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(albumRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(trackRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void curatedImportKeepsAlbumsAndEpsButRemovesCompilations() throws Exception {
        String json = """
                {
                  "release-groups": [
                    {"id":"album-1","title":"First","primary-type":"Album","first-release-date":"1970-01-01"},
                    {"id":"single-1","title":"Single","primary-type":"Single","first-release-date":"1969-01-01"},
                    {"id":"compilation-1","title":"Best Of","primary-type":"Album","secondary-types":["Compilation"]},
                    {"id":"ep-1","title":"Small","primary-type":"EP","first-release-date":"1971-01-01"}
                  ]
                }
                """;

        var groups = musicBrainzService.selectAlbumGroups(new ObjectMapper().readTree(json), 5);

        assertEquals(2, groups.size());
        assertEquals("album-1", groups.get(0).path("id").asText());
        assertEquals("ep-1", groups.get(1).path("id").asText());
    }

    @Test
    void normalizesMusicBrainzGenresForProfileStatistics() {
        assertEquals("Classical", musicBrainzService.normalizeGenre("modern classical"));
        assertEquals("Progressive Rock", musicBrainzService.normalizeGenre("prog rock"));
        assertEquals("Jazz", musicBrainzService.normalizeGenre("jazz"));
    }
}
