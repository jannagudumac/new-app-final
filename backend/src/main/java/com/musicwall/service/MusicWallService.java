package com.musicwall.service;

import com.musicwall.dto.CreateMusicWallRequest;
import com.musicwall.dto.MusicWallDTO;
import com.musicwall.dto.MusicWallDetailDTO;
import com.musicwall.dto.UpdateWallAppearanceRequest;
import com.musicwall.entity.MusicWallEntity;
import com.musicwall.entity.UserEntity;
import com.musicwall.entity.WallMembershipEntity;
import com.musicwall.entity.WallRole;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.MusicWallRepository;
import com.musicwall.repository.MusicItemRepository;
import com.musicwall.repository.MusicSectionRepository;
import com.musicwall.repository.UserRepository;
import com.musicwall.repository.WallMembershipRepository;
import com.musicwall.repository.WallInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MusicWallService {

    private final MusicWallRepository musicWallRepository;
    private final UserRepository userRepository;
    private final MusicSectionRepository musicSectionRepository;
    private final MusicItemRepository musicItemRepository;
    private final MusicSectionService musicSectionService;
    private final WallMembershipRepository wallMembershipRepository;
    private final WallAccessService wallAccessService;
    private final WallInvitationRepository wallInvitationRepository;

    public MusicWallService(
            MusicWallRepository musicWallRepository,
            UserRepository userRepository,
            MusicSectionRepository musicSectionRepository,
            MusicItemRepository musicItemRepository,
            MusicSectionService musicSectionService,
            WallMembershipRepository wallMembershipRepository,
            WallAccessService wallAccessService,
            WallInvitationRepository wallInvitationRepository
    ) {
        this.musicWallRepository = musicWallRepository;
        this.userRepository = userRepository;
        this.musicSectionRepository = musicSectionRepository;
        this.musicItemRepository = musicItemRepository;
        this.musicSectionService = musicSectionService;
        this.wallMembershipRepository = wallMembershipRepository;
        this.wallAccessService = wallAccessService;
        this.wallInvitationRepository = wallInvitationRepository;
    }

    @Transactional
    public MusicWallDTO createWall(String username, CreateMusicWallRequest request) {
        UserEntity owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MusicWallEntity wall = new MusicWallEntity();
        wall.setName(request.getName());
        wall.setDescription(request.getDescription());
        wall.setWallpaper(normalizeWallpaper(request.getWallpaper()));
        wall.setWallColor(normalizeWallColor(request.getWallColor()));
        wall.setOwner(owner);

        MusicWallEntity savedWall = musicWallRepository.save(wall);
        WallMembershipEntity membership = new WallMembershipEntity();
        membership.setWall(savedWall);
        membership.setUser(owner);
        membership.setRole(WallRole.OWNER);
        wallMembershipRepository.save(membership);
        return convertToDTO(savedWall);
    }

    public List<MusicWallDTO> getWallsForUser(String username) {
        Map<Long, MusicWallEntity> uniqueWalls = new LinkedHashMap<>();
        musicWallRepository.findByOwnerUsernameOrderByIdDesc(username)
                .forEach(wall -> uniqueWalls.put(wall.getId(), wall));
        wallMembershipRepository.findByUserUsernameOrderByWallIdDesc(username)
                .forEach(membership -> uniqueWalls.putIfAbsent(membership.getWall().getId(), membership.getWall()));
        List<MusicWallEntity> walls = new ArrayList<>(uniqueWalls.values());

        List<MusicWallDTO> wallDTOs = new ArrayList<>();
        for (MusicWallEntity wall : walls) {
            wallDTOs.add(convertToDTO(wall));
        }

        return wallDTOs;
    }

    public MusicWallDetailDTO getWall(Long id, String username) {
        MusicWallEntity wall = wallAccessService.findAccessibleWall(username, id);

        MusicWallDetailDTO dto = new MusicWallDetailDTO();
        dto.setId(wall.getId());
        dto.setName(wall.getName());
        dto.setDescription(wall.getDescription());
        dto.setOwnerUsername(wall.getOwner().getUsername());
        dto.setWallpaper(normalizeWallpaper(wall.getWallpaper()));
        dto.setWallColor(normalizeWallColor(wall.getWallColor()));
        dto.setSections(musicSectionService.getSectionsForWall(username, id));
        return dto;
    }

    public MusicWallDTO updateWall(
            Long id,
            String username,
            CreateMusicWallRequest request
    ) {
        MusicWallEntity wall = wallAccessService.findOwnedWall(username, id);

        wall.setName(request.getName());
        wall.setDescription(request.getDescription());
        wall.setWallpaper(normalizeWallpaper(request.getWallpaper()));
        wall.setWallColor(normalizeWallColor(request.getWallColor()));
        return convertToDTO(musicWallRepository.save(wall));
    }

    public MusicWallDTO updateWallAppearance(
            Long id,
            String username,
            UpdateWallAppearanceRequest request
    ) {
        MusicWallEntity wall = wallAccessService.findAccessibleWall(username, id);
        wall.setWallpaper(normalizeWallpaper(request.getWallpaper()));
        wall.setWallColor(normalizeWallColor(request.getWallColor()));
        return convertToDTO(musicWallRepository.save(wall));
    }

    @Transactional
    public void deleteWall(Long id, String username) {
        MusicWallEntity wall = wallAccessService.findOwnedWall(username, id);

        musicSectionRepository.findByWallIdOrderByIdAsc(id).forEach(section ->
                musicItemRepository.deleteBySectionId(section.getId())
        );
        musicSectionRepository.deleteByWallId(id);
        wallInvitationRepository.deleteByWallId(id);
        wallMembershipRepository.deleteByWallId(id);
        musicWallRepository.delete(wall);
    }

    private MusicWallDTO convertToDTO(MusicWallEntity wall) {
        MusicWallDTO dto = new MusicWallDTO();
        dto.setId(wall.getId());
        dto.setName(wall.getName());
        dto.setDescription(wall.getDescription());
        dto.setOwnerUsername(wall.getOwner().getUsername());
        dto.setWallpaper(normalizeWallpaper(wall.getWallpaper()));
        dto.setWallColor(normalizeWallColor(wall.getWallColor()));
        return dto;
    }

    private String normalizeWallpaper(String wallpaper) {
        if (wallpaper == null || wallpaper.isBlank()) {
            return "NONE";
        }

        String migratedWallpaper = switch (wallpaper) {
            case "MIDNIGHT_CASSETTES" -> "IMAGE_1";
            case "MINT_TAPE_GRID" -> "IMAGE_2";
            case "ACOUSTIC_PASTELS" -> "IMAGE_3";
            case "SCHOOL_JAM" -> "IMAGE_4";
            case "CARNIVAL_NIGHT" -> "IMAGE_5";
            case "MUSIC_DOODLES" -> "IMAGE_6";
            case "RETRO_HIFI" -> "IMAGE_7";
            case "VINTAGE_SOUND" -> "IMAGE_8";
            default -> wallpaper;
        };

        if (List.of(
                "CORK", "PLASTER", "BRICK", "BOTANICAL", "CONCERT",
                "NOTES", "VINYL", "MICROPHONES", "GUITARS", "PIANO",
                "CASSETTES", "HEADPHONES", "DRUMS", "EQUALIZER", "STUDIO"
        ).contains(migratedWallpaper)) {
            return "NONE";
        }
        return migratedWallpaper;
    }

    private String normalizeWallColor(String wallColor) {
        if (wallColor == null || wallColor.isBlank()) {
            return "#FFFFFF";
        }
        return wallColor;
    }
}
