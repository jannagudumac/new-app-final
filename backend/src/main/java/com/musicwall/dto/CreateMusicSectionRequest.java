package com.musicwall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateMusicSectionRequest {

    @NotBlank(message = "Section name is required")
    @Size(max = 80, message = "Section name is too long")
    private String name;

    @Pattern(
            regexp = "CREAM|ROSE|PEACH|MINT|SKY|LAVENDER",
            message = "Unknown note color"
    )
    private String noteColor = "CREAM";

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
}
