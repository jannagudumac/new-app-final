package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class MusicWallDetailDTO {

    private Long id;
    private String name;
    private String description;
    private String ownerUsername;
    private String wallpaper;
    private String wallColor;
    private List<MusicSectionDTO> sections = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

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

    public List<MusicSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<MusicSectionDTO> sections) {
        this.sections = sections;
    }
}
