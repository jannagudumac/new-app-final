package com.musicwall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateMusicItemRequest {

    @NotBlank(message = "Listening status is required")
    @Pattern(regexp = "TO_LISTEN|LISTENED", message = "Invalid listening status")
    private String status;

    private Long catalogTrackId;
    private Long catalogAlbumId;

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
