package com.musicwall.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wall_invitation")
public class WallInvitationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "wall_id", nullable = false)
    private MusicWallEntity wall;
    @ManyToOne @JoinColumn(name = "invited_user_id", nullable = false)
    private UserEntity invitedUser;
    @ManyToOne @JoinColumn(name = "invited_by_id", nullable = false)
    private UserEntity invitedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MusicWallEntity getWall() { return wall; }
    public void setWall(MusicWallEntity wall) { this.wall = wall; }
    public UserEntity getInvitedUser() { return invitedUser; }
    public void setInvitedUser(UserEntity invitedUser) { this.invitedUser = invitedUser; }
    public UserEntity getInvitedBy() { return invitedBy; }
    public void setInvitedBy(UserEntity invitedBy) { this.invitedBy = invitedBy; }
    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
