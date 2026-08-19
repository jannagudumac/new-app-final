package com.musicwall.dto;

import java.util.ArrayList;
import java.util.List;

public class MusicSectionDTO {

    private Long id;
    private String name;
    private String noteColor;
    private List<MusicItemDTO> items = new ArrayList<>();

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

    public String getNoteColor() {
        return noteColor;
    }

    public void setNoteColor(String noteColor) {
        this.noteColor = noteColor;
    }

    public List<MusicItemDTO> getItems() {
        return items;
    }

    public void setItems(List<MusicItemDTO> items) {
        this.items = items;
    }
}
