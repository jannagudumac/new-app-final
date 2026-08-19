package com.musicwall.dto;

public class UserSearchDTO {
    private String username;
    private String friendshipStatus;

    public UserSearchDTO() {
    }

    public UserSearchDTO(String username, String friendshipStatus) {
        this.username = username;
        this.friendshipStatus = friendshipStatus;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
}
