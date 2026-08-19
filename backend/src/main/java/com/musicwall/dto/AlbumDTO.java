package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class AlbumDTO {

    private Long id;
    private String musicBrainzId;
    private String title;
    private Integer releaseYear;
    private String coverUrl;
    private Long artistId;
    private String artistMusicBrainzId;
    private String artistName;
    private List<GenreDTO> genres = new ArrayList<>();
    private List<TrackDTO> tracks = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMusicBrainzId() { return musicBrainzId; }

    public void setMusicBrainzId(String musicBrainzId) { this.musicBrainzId = musicBrainzId; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getArtistMusicBrainzId() { return artistMusicBrainzId; }
    public void setArtistMusicBrainzId(String artistMusicBrainzId) { this.artistMusicBrainzId = artistMusicBrainzId; }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public List<GenreDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<GenreDTO> genres) {
        this.genres = genres;
    }

    public List<TrackDTO> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackDTO> tracks) {
        this.tracks = tracks;
    }
}
