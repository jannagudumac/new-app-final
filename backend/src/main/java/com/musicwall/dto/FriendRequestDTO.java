package com.musicwall.dto;

public class FriendRequestDTO {
    private Long id;
    private String username;

    public FriendRequestDTO() {
    }

    public FriendRequestDTO(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
