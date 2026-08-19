package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class FavouriteIdsDTO {
    private List<Long> artists = new ArrayList<>();
    private List<Long> albums = new ArrayList<>();
    private List<Long> tracks = new ArrayList<>();

    public List<Long> getArtists() { return artists; }
    public void setArtists(List<Long> artists) { this.artists = artists; }
    public List<Long> getAlbums() { return albums; }
    public void setAlbums(List<Long> albums) { this.albums = albums; }
    public List<Long> getTracks() { return tracks; }
    public void setTracks(List<Long> tracks) { this.tracks = tracks; }
}
