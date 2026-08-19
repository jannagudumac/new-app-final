package com.musicwall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateMusicItemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title is too long")
    private String title;

    @NotBlank(message = "Artist is required")
    @Size(max = 120, message = "Artist name is too long")
    private String artist;

    @NotBlank(message = "Item type is required")
    @Pattern(regexp = "TRACK|ALBUM", message = "Item type must be TRACK or ALBUM")
    private String itemType;

    @NotBlank(message = "Listening status is required")
    @Pattern(regexp = "TO_LISTEN|LISTENED", message = "Invalid listening status")
    private String status;

    private Long catalogTrackId;
    private Long catalogAlbumId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCatalogTrackId() { return catalogTrackId; }
    public void setCatalogTrackId(Long catalogTrackId) { this.catalogTrackId = catalogTrackId; }
    public Long getCatalogAlbumId() { return catalogAlbumId; }
    public void setCatalogAlbumId(Long catalogAlbumId) { this.catalogAlbumId = catalogAlbumId; }
}
