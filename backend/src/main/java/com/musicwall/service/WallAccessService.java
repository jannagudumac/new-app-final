package com.musicwall.service;
import com.musicwall.entity.MusicWallEntity;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.MusicWallRepository;
import com.musicwall.repository.WallMembershipRepository;
import org.springframework.stereotype.Service;

@Service
public class WallAccessService {
    private final MusicWallRepository wallRepository;
    private final WallMembershipRepository membershipRepository;
    public WallAccessService(MusicWallRepository wallRepository, WallMembershipRepository membershipRepository) {
        this.wallRepository = wallRepository; this.membershipRepository = membershipRepository;
    }
    public MusicWallEntity findAccessibleWall(String username, Long wallId) {
        MusicWallEntity wall = wallRepository.findById(wallId)
                .orElseThrow(() -> new ResourceNotFoundException("Wall not found"));
        if (wall.getOwner().getUsername().equals(username) || membershipRepository.existsByWallIdAndUserUsername(wallId, username)) return wall;
        throw new ResourceNotFoundException("Wall not found");
    }
    public MusicWallEntity findOwnedWall(String username, Long wallId) {
        return wallRepository.findByIdAndOwnerUsername(wallId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Wall not found"));
    }
}
