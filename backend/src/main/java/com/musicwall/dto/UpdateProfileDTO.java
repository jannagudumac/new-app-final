package com.musicwall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileDTO {
    @NotBlank(message = "Display name is required")
    @Size(max = 60, message = "Display name must be at most 60 characters")
    private String displayName;

    @Size(max = 240, message = "Bio must be at most 240 characters")
    private String bio;

    private boolean showArtists;
    private boolean showAlbums;
    private boolean showTracks;
    private boolean showTasteProfile;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public boolean isShowArtists() { return showArtists; }
    public void setShowArtists(boolean showArtists) { this.showArtists = showArtists; }
    public boolean isShowAlbums() { return showAlbums; }
    public void setShowAlbums(boolean showAlbums) { this.showAlbums = showAlbums; }
    public boolean isShowTracks() { return showTracks; }
    public void setShowTracks(boolean showTracks) { this.showTracks = showTracks; }
    public boolean isShowTasteProfile() { return showTasteProfile; }
    public void setShowTasteProfile(boolean showTasteProfile) { this.showTasteProfile = showTasteProfile; }
}
