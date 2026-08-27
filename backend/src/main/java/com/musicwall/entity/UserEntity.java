package com.musicwall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;


    @Column(length = 240)
    private String bio;

    @Column(columnDefinition = "bytea")
    private byte[] avatarImage;

    private String avatarContentType;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean showArtists = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean showAlbums = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean showTracks = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean showTasteProfile = true;

    public UserEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public byte[] getAvatarImage() { return avatarImage; }
    public void setAvatarImage(byte[] avatarImage) { this.avatarImage = avatarImage; }
    public String getAvatarContentType() { return avatarContentType; }
    public void setAvatarContentType(String avatarContentType) { this.avatarContentType = avatarContentType; }
    public boolean isShowArtists() { return showArtists; }
    public void setShowArtists(boolean showArtists) { this.showArtists = showArtists; }
    public boolean isShowAlbums() { return showAlbums; }
    public void setShowAlbums(boolean showAlbums) { this.showAlbums = showAlbums; }
    public boolean isShowTracks() { return showTracks; }
    public void setShowTracks(boolean showTracks) { this.showTracks = showTracks; }
    public boolean isShowTasteProfile() { return showTasteProfile; }
    public void setShowTasteProfile(boolean showTasteProfile) { this.showTasteProfile = showTasteProfile; }
}
