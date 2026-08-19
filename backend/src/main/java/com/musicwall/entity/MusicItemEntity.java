package com.musicwall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "music_item")
public class MusicItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private MusicItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListeningStatus status;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private MusicSectionEntity section;

    @ManyToOne
    @JoinColumn(name = "catalog_track_id")
    private TrackEntity catalogTrack;

    @ManyToOne
    @JoinColumn(name = "catalog_album_id")
    private AlbumEntity catalogAlbum;

    public MusicItemEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public MusicItemType getItemType() {
        return itemType;
    }

    public void setItemType(MusicItemType itemType) {
        this.itemType = itemType;
    }

    public ListeningStatus getStatus() {
        return status;
    }

    public void setStatus(ListeningStatus status) {
        this.status = status;
    }

    public MusicSectionEntity getSection() {
        return section;
    }

    public void setSection(MusicSectionEntity section) {
        this.section = section;
    }

    public TrackEntity getCatalogTrack() { return catalogTrack; }
    public void setCatalogTrack(TrackEntity catalogTrack) { this.catalogTrack = catalogTrack; }
    public AlbumEntity getCatalogAlbum() { return catalogAlbum; }
    public void setCatalogAlbum(AlbumEntity catalogAlbum) { this.catalogAlbum = catalogAlbum; }
}
