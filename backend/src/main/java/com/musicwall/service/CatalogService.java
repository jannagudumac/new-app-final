package com.musicwall.service;

import com.musicwall.dto.AlbumDTO;
import com.musicwall.dto.ArtistDTO;
import com.musicwall.dto.ArtistDetailDTO;
import com.musicwall.dto.CatalogSearchDTO;
import com.musicwall.dto.CatalogSuggestionDTO;
import com.musicwall.dto.GenreDTO;
import com.musicwall.dto.TrackDTO;
import com.musicwall.entity.AlbumEntity;
import com.musicwall.entity.ArtistEntity;
import com.musicwall.entity.GenreEntity;
import com.musicwall.entity.TrackEntity;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.AlbumRepository;
import com.musicwall.repository.ArtistRepository;
import com.musicwall.repository.TrackRepository;
import com.musicwall.repository.GenreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final GenreRepository genreRepository;

    public CatalogService(
            ArtistRepository artistRepository,
            AlbumRepository albumRepository,
            TrackRepository trackRepository,
            GenreRepository genreRepository
    ) {
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.trackRepository = trackRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional(readOnly = true)
    public CatalogSearchDTO search(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        CatalogSearchDTO result = new CatalogSearchDTO();

        if (cleanQuery.isEmpty()) {
            result.setArtists(artistRepository.findAllByOrderByNameAsc().stream()
                    .map(this::convertArtist).toList());
            result.setAlbums(albumRepository.findAllByOrderByTitleAsc().stream()
                    .map(this::convertAlbum).toList());
            result.setTracks(trackRepository.findAllByOrderByTitleAsc().stream()
                    .map(this::convertTrack).toList());
            result.setGenres(genreRepository.findAllByOrderByNameAsc().stream()
                    .map(this::convertGenre).toList());
            return result;
        }

        result.setArtists(artistRepository.searchSimilar(cleanQuery).stream()
                .map(this::convertArtist).toList());
        result.setAlbums(albumRepository.searchSimilar(cleanQuery).stream()
                .map(this::convertAlbum).toList());
        result.setTracks(trackRepository.searchSimilar(cleanQuery).stream()
                .map(this::convertTrack).toList());
        result.setGenres(genreRepository.findByNameContainingIgnoreCaseOrderByNameAsc(cleanQuery)
                .stream().map(this::convertGenre).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public List<CatalogSuggestionDTO> getSuggestions(String query) {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.length() < 2) {
            return List.of();
        }

        List<com.musicwall.dto.CatalogSuggestionProjection> rows = new java.util.ArrayList<>();
        rows.addAll(artistRepository.findSuggestions(cleanQuery, 5));
        rows.addAll(albumRepository.findSuggestions(cleanQuery, 5));
        rows.addAll(trackRepository.findSuggestions(cleanQuery, 5));
        return rows.stream()
                .sorted((first, second) -> Float.compare(second.getScore(), first.getScore()))
                .limit(10)
                .map(row -> new CatalogSuggestionDTO(
                        row.getId().toString(), row.getType(), row.getTitle(), row.getSubtitle()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtistDetailDTO getArtist(Long id) {
        ArtistEntity artist = artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found"));

        ArtistDetailDTO detail = new ArtistDetailDTO();
        detail.setArtist(convertArtist(artist));
        detail.setAlbums(albumRepository.findByArtistIdOrderByReleaseYearAscTitleAsc(id)
                .stream().map(this::convertAlbum).toList());
        detail.setTracks(trackRepository.findByArtistIdOrderByTitleAsc(id)
                .stream().map(this::convertTrack).toList());
        return detail;
    }

    @Transactional(readOnly = true)
    public AlbumDTO getAlbum(Long id) {
        AlbumDTO album = convertAlbum(albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found")));
        album.setTracks(trackRepository.findByAlbumIdOrderByTitleAsc(id)
                .stream()
                .map(this::convertTrack)
                .toList());
        return album;
    }

    @Transactional(readOnly = true)
    public TrackDTO getTrack(Long id) {
        return convertTrack(trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Track not found")));
    }

    private ArtistDTO convertArtist(ArtistEntity artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setMusicBrainzId(artist.getMusicBrainzId());
        dto.setName(artist.getName());
        return dto;
    }

    private GenreDTO convertGenre(GenreEntity genre) {
        GenreDTO dto = new GenreDTO();
        dto.setId(genre.getId());
        dto.setName(genre.getName());
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
        dto.setArtistName(album.getArtist().getName());
        dto.setGenres(album.getGenres().stream()
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .map(this::convertGenre)
                .toList());
        return dto;
    }

    private TrackDTO convertTrack(TrackEntity track) {
        TrackDTO dto = new TrackDTO();
        dto.setId(track.getId());
        dto.setMusicBrainzId(track.getMusicBrainzId());
        dto.setTitle(track.getTitle());
        dto.setDurationSeconds(track.getDurationSeconds());
        dto.setArtistId(track.getArtist().getId());
        dto.setArtistName(track.getArtist().getName());
        if (track.getAlbum() != null) {
            dto.setAlbumId(track.getAlbum().getId());
            dto.setAlbumTitle(track.getAlbum().getTitle());
            dto.setAlbumCoverUrl(track.getAlbum().getCoverUrl());
        }
        dto.setGenres(track.getGenres().stream()
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .map(this::convertGenre)
                .toList());
        return dto;
    }
}
