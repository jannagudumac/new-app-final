package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class TrackDTO {

    private Long id;
    private String musicBrainzId;
    private String title;
    private Integer durationSeconds;
    private Long artistId;
    private String artistMusicBrainzId;
    private String artistName;
    private Long albumId;
    private String albumMusicBrainzId;
    private String albumTitle;
    private String albumCoverUrl;
    private List<GenreDTO> genres = new ArrayList<>();

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

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
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

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public String getAlbumMusicBrainzId() { return albumMusicBrainzId; }
    public void setAlbumMusicBrainzId(String albumMusicBrainzId) { this.albumMusicBrainzId = albumMusicBrainzId; }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }

    public String getAlbumCoverUrl() {
        return albumCoverUrl;
    }

    public void setAlbumCoverUrl(String albumCoverUrl) {
        this.albumCoverUrl = albumCoverUrl;
    }

    public List<GenreDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<GenreDTO> genres) {
        this.genres = genres;
    }
}
