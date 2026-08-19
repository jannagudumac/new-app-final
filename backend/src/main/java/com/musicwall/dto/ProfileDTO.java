package com.musicwall.dto;
import java.util.ArrayList;
import java.util.List;
public class ProfileDTO {
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private boolean showArtists;
    private boolean showAlbums;
    private boolean showTracks;
    private boolean showTasteProfile;
    private List<ArtistDTO> favouriteArtists = new ArrayList<>();
    private List<AlbumDTO> favouriteAlbums = new ArrayList<>();
    private List<TrackDTO> favouriteTracks = new ArrayList<>();
    private List<GenreStatDTO> genreStatistics = new ArrayList<>();
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public boolean isShowArtists() { return showArtists; }
    public void setShowArtists(boolean showArtists) { this.showArtists = showArtists; }
    public boolean isShowAlbums() { return showAlbums; }
    public void setShowAlbums(boolean showAlbums) { this.showAlbums = showAlbums; }
    public boolean isShowTracks() { return showTracks; }
    public void setShowTracks(boolean showTracks) { this.showTracks = showTracks; }
    public boolean isShowTasteProfile() { return showTasteProfile; }
    public void setShowTasteProfile(boolean showTasteProfile) { this.showTasteProfile = showTasteProfile; }
    public List<ArtistDTO> getFavouriteArtists() { return favouriteArtists; }
    public void setFavouriteArtists(List<ArtistDTO> favouriteArtists) { this.favouriteArtists = favouriteArtists; }
    public List<AlbumDTO> getFavouriteAlbums() { return favouriteAlbums; }
    public void setFavouriteAlbums(List<AlbumDTO> favouriteAlbums) { this.favouriteAlbums = favouriteAlbums; }
    public List<TrackDTO> getFavouriteTracks() { return favouriteTracks; }
    public void setFavouriteTracks(List<TrackDTO> favouriteTracks) { this.favouriteTracks = favouriteTracks; }
    public List<GenreStatDTO> getGenreStatistics() { return genreStatistics; }
    public void setGenreStatistics(List<GenreStatDTO> genreStatistics) { this.genreStatistics = genreStatistics; }
}
