package com.musicwall.dto;

public class CatalogSuggestionDTO {

    private String id;
    private String type;
    private String title;
    private String subtitle;

    public CatalogSuggestionDTO() {
    }

    public CatalogSuggestionDTO(String id, String type, String title, String subtitle) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
