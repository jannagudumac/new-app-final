package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class CatalogImportResultDTO {

    private int artists;
    private int albums;
    private int tracks;
    private List<String> errors = new ArrayList<>();

    public int getArtists() {
        return artists;
    }

    public void addArtist() {
        artists++;
    }

    public int getAlbums() {
        return albums;
    }

    public void addAlbums(int amount) {
        albums += amount;
    }

    public int getTracks() {
        return tracks;
    }

    public void addTracks(int amount) {
        tracks += amount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }
}
