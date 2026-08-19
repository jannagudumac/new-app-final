package com.musicwall.dto;

public class ArtistDTO {

    private Long id;
    private String musicBrainzId;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMusicBrainzId() { return musicBrainzId; }

    public void setMusicBrainzId(String musicBrainzId) { this.musicBrainzId = musicBrainzId; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
