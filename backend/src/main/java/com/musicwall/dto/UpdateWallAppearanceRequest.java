package com.musicwall.dto;

import jakarta.validation.constraints.Pattern;

public class UpdateWallAppearanceRequest {

    @Pattern(
            regexp = "NONE|IMAGE_1|IMAGE_2|IMAGE_3|IMAGE_4|IMAGE_5|IMAGE_6|IMAGE_7|IMAGE_8|IMAGE_9",
            message = "Unknown wallpaper"
    )
    private String wallpaper = "NONE";

    @Pattern(
            regexp = "#[0-9a-fA-F]{6}",
            message = "Wall color must be a hexadecimal color"
    )
    private String wallColor = "#FFFFFF";

    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(String wallpaper) {
        this.wallpaper = wallpaper;
    }

    public String getWallColor() {
        return wallColor;
    }

    public void setWallColor(String wallColor) {
        this.wallColor = wallColor;
    }
}
