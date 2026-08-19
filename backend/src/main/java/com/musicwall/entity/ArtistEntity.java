package com.musicwall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "artist",
        uniqueConstraints = @UniqueConstraint(name = "uk_artist_name", columnNames = "name")
)
public class ArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "musicbrainz_id", unique = true, length = 36)
    private String musicBrainzId;

    @Column(name = "catalog_imported", nullable = false, columnDefinition = "boolean default false")
    private boolean catalogImported;

    public ArtistEntity() {
    }

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

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public void setMusicBrainzId(String musicBrainzId) {
        this.musicBrainzId = musicBrainzId;
    }

    public boolean isCatalogImported() {
        return catalogImported;
    }

    public void setCatalogImported(boolean catalogImported) {
        this.catalogImported = catalogImported;
    }
}
