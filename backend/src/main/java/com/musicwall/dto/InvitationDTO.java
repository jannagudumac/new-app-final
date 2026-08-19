package com.musicwall.dto;
public class InvitationDTO {
    private Long id;
    private Long wallId;
    private String wallName;
    private String invitedByUsername;
    private String status;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWallId() { return wallId; }
    public void setWallId(Long wallId) { this.wallId = wallId; }
    public String getWallName() { return wallName; }
    public void setWallName(String wallName) { this.wallName = wallName; }
    public String getInvitedByUsername() { return invitedByUsername; }
    public void setInvitedByUsername(String invitedByUsername) { this.invitedByUsername = invitedByUsername; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
