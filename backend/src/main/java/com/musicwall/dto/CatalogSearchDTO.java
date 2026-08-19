package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class CatalogSearchDTO {

    private List<ArtistDTO> artists = new ArrayList<>();
    private List<AlbumDTO> albums = new ArrayList<>();
    private List<TrackDTO> tracks = new ArrayList<>();
    private List<GenreDTO> genres = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public List<ArtistDTO> getArtists() {
        return artists;
    }

    public void setArtists(List<ArtistDTO> artists) {
        this.artists = artists;
    }

    public List<AlbumDTO> getAlbums() {
        return albums;
    }

    public void setAlbums(List<AlbumDTO> albums) {
        this.albums = albums;
    }

    public List<TrackDTO> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackDTO> tracks) {
        this.tracks = tracks;
    }

    public List<GenreDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<GenreDTO> genres) {
        this.genres = genres;
    }

    public List<String> getWarnings() { return warnings; }

    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
