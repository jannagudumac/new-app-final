package com.musicwall.service;

import com.musicwall.dto.AlbumDTO;
import com.musicwall.dto.ArtistDTO;
import com.musicwall.dto.CatalogSearchDTO;
import com.musicwall.dto.CatalogSuggestionDTO;
import com.musicwall.dto.TrackDTO;
import com.musicwall.entity.AlbumEntity;
import com.musicwall.entity.ArtistEntity;
import com.musicwall.entity.TrackEntity;
import com.musicwall.repository.AlbumRepository;
import com.musicwall.repository.ArtistRepository;
import com.musicwall.repository.TrackRepository;
import com.musicwall.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private GenreRepository genreRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                artistRepository,
                albumRepository,
                trackRepository,
                genreRepository
        );
    }

    @Test
    void emptySearchUsesTheLocalCatalogue() {
        when(artistRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(albumRepository.findAllByOrderByTitleAsc()).thenReturn(List.of());
        when(trackRepository.findAllByOrderByTitleAsc()).thenReturn(List.of());
        when(genreRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        CatalogSearchDTO result = catalogService.search("   ");

        assertEquals(0, result.getArtists().size());
        verify(artistRepository).findAllByOrderByNameAsc();
    }

    @Test
    void suggestionsIgnoreQueriesShorterThanTwoCharacters() {
        List<CatalogSuggestionDTO> result = catalogService.getSuggestions(" a ");

        assertEquals(0, result.size());
        verify(artistRepository, never()).findSuggestions(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void catalogueSearchDoesNotCutResultsAtFifty() {
        List<ArtistEntity> artists = IntStream.rangeClosed(1, 51).mapToObj(index -> {
            ArtistEntity artist = new ArtistEntity();
            artist.setId((long) index);
            artist.setName("Artist " + index);
            return artist;
        }).toList();
        when(artistRepository.searchSimilar("artist")).thenReturn(artists);
        when(albumRepository.searchSimilar("artist")).thenReturn(List.of());
        when(trackRepository.searchSimilar("artist")).thenReturn(List.of());
        when(genreRepository.findByNameContainingIgnoreCaseOrderByNameAsc("artist")).thenReturn(List.of());

        CatalogSearchDTO result = catalogService.search("artist");

        assertEquals(51, result.getArtists().size());
    }

    @Test
    void suggestionsUseLocalPostgreSqlResults() {
        var artist = suggestion(1L, "ARTIST", "The Beatles", "Artist", 1.5f);
        var album = suggestion(2L, "ALBUM", "Beatles for Sale", "The Beatles", 1.2f);
        var track = suggestion(3L, "TRACK", "Beat It", "Michael Jackson", 1.0f);
        when(artistRepository.findSuggestions("beatels", 5)).thenReturn(List.of(artist));
        when(albumRepository.findSuggestions("beatels", 5)).thenReturn(List.of(album));
        when(trackRepository.findSuggestions("beatels", 5)).thenReturn(List.of(track));

        List<CatalogSuggestionDTO> result = catalogService.getSuggestions(" beatels ");

        assertEquals(3, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("The Beatles", result.get(0).getTitle());
        assertEquals("ALBUM", result.get(1).getType());
        assertEquals("Beat It", result.get(2).getTitle());
    }

    @Test
    void albumDetailContainsItsTracks() {
        ArtistEntity artist = new ArtistEntity();
        artist.setId(4L);
        artist.setName("David Bowie");

        AlbumEntity album = new AlbumEntity();
        album.setId(8L);
        album.setTitle("Diamond Dogs");
        album.setArtist(artist);

        TrackEntity track = new TrackEntity();
        track.setId(15L);
        track.setTitle("Rebel Rebel");
        track.setArtist(artist);
        track.setAlbum(album);

        when(albumRepository.findById(8L)).thenReturn(Optional.of(album));
        when(trackRepository.findByAlbumIdOrderByTitleAsc(8L)).thenReturn(List.of(track));

        AlbumDTO result = catalogService.getAlbum(8L);

        assertEquals(1, result.getTracks().size());
        assertEquals("Rebel Rebel", result.getTracks().get(0).getTitle());
        verify(trackRepository).findByAlbumIdOrderByTitleAsc(8L);
    }

    private com.musicwall.dto.CatalogSuggestionProjection suggestion(
            Long id, String type, String title, String subtitle, Float score
    ) {
        return new com.musicwall.dto.CatalogSuggestionProjection() {
            public Long getId() { return id; }
            public String getType() { return type; }
            public String getTitle() { return title; }
            public String getSubtitle() { return subtitle; }
            public Float getScore() { return score; }
        };
    }

    private CatalogSearchDTO emptyResult() {
        CatalogSearchDTO result = new CatalogSearchDTO();
        result.setArtists(List.of());
        result.setAlbums(List.of());
        result.setTracks(List.of());
        result.setGenres(List.of());
        return result;
    }
}
