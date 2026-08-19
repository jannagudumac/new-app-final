package com.musicwall.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wall_membership", uniqueConstraints = @UniqueConstraint(columnNames = {"wall_id", "user_id"}))
public class WallMembershipEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "wall_id", nullable = false)
    private MusicWallEntity wall;
    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WallRole role;
    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MusicWallEntity getWall() { return wall; }
    public void setWall(MusicWallEntity wall) { this.wall = wall; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public WallRole getRole() { return role; }
    public void setRole(WallRole role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
