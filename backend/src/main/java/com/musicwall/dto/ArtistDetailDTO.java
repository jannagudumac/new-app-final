package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class ArtistDetailDTO {

    private ArtistDTO artist;
    private List<AlbumDTO> albums = new ArrayList<>();
    private List<TrackDTO> tracks = new ArrayList<>();

    public ArtistDTO getArtist() {
        return artist;
    }

    public void setArtist(ArtistDTO artist) {
        this.artist = artist;
    }

    public List<AlbumDTO> getAlbums() {
        return albums;
    }

    public void setAlbums(List<AlbumDTO> albums) {
        this.albums = albums;
    }

    public List<TrackDTO> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackDTO> tracks) {
        this.tracks = tracks;
    }
}
