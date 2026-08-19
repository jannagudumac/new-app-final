package com.musicwall.dto;

public class CuratedArtistDTO {

    private String name;
    private String musicBrainzId;
    private int maxAlbums = 5;

    public CuratedArtistDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public void setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
    }

    public int getMaxAlbums() {
        return maxAlbums;
    }

    public void setMaxAlbums(int maxAlbums) {
        this.maxAlbums = maxAlbums;
    }
}
