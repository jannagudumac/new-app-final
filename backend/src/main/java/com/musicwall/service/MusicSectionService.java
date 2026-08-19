package com.musicwall.service;

import com.musicwall.dto.CreateMusicSectionRequest;
import com.musicwall.dto.MusicItemDTO;
import com.musicwall.dto.MusicSectionDTO;
import com.musicwall.entity.MusicItemEntity;
import com.musicwall.entity.MusicSectionEntity;
import com.musicwall.entity.MusicWallEntity;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.MusicItemRepository;
import com.musicwall.repository.MusicSectionRepository;
import com.musicwall.repository.MusicWallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MusicSectionService {

    private final MusicSectionRepository musicSectionRepository;
    private final MusicItemRepository musicItemRepository;
    private final WallAccessService wallAccessService;

    public MusicSectionService(
            MusicSectionRepository musicSectionRepository,
            MusicItemRepository musicItemRepository,
            WallAccessService wallAccessService
    ) {
        this.musicSectionRepository = musicSectionRepository;
        this.musicItemRepository = musicItemRepository;
        this.wallAccessService = wallAccessService;
    }

    public MusicSectionDTO createSection(
            String username,
            Long wallId,
            CreateMusicSectionRequest request
    ) {
        MusicWallEntity wall = findAccessibleWall(username, wallId);

        MusicSectionEntity section = new MusicSectionEntity();
        section.setName(request.getName());
        section.setNoteColor(normalizeNoteColor(request.getNoteColor()));
        section.setWall(wall);

        return convertToDTO(musicSectionRepository.save(section));
    }

    public List<MusicSectionDTO> getSectionsForWall(String username, Long wallId) {
        findAccessibleWall(username, wallId);

        List<MusicSectionEntity> sections =
                musicSectionRepository.findByWallIdOrderByIdAsc(wallId);
        List<MusicSectionDTO> sectionDTOs = new ArrayList<>();

        for (MusicSectionEntity section : sections) {
            sectionDTOs.add(convertToDTO(section));
        }

        return sectionDTOs;
    }

    public MusicSectionDTO updateSection(
            String username,
            Long wallId,
            Long sectionId,
            CreateMusicSectionRequest request
    ) {
        findAccessibleWall(username, wallId);
        MusicSectionEntity section = findSectionInWall(sectionId, wallId);
        section.setName(request.getName());
        section.setNoteColor(normalizeNoteColor(request.getNoteColor()));
        return convertToDTO(musicSectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(String username, Long wallId, Long sectionId) {
        findAccessibleWall(username, wallId);
        MusicSectionEntity section = findSectionInWall(sectionId, wallId);
        musicItemRepository.deleteBySectionId(section.getId());
        musicSectionRepository.delete(section);
    }

    private MusicWallEntity findAccessibleWall(String username, Long wallId) {
        return wallAccessService.findAccessibleWall(username, wallId);
    }

    private MusicSectionEntity findSectionInWall(Long sectionId, Long wallId) {
        MusicSectionEntity section = musicSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (!section.getWall().getId().equals(wallId)) {
            throw new ResourceNotFoundException("Section not found");
        }

        return section;
    }

    private MusicSectionDTO convertToDTO(MusicSectionEntity section) {
        MusicSectionDTO dto = new MusicSectionDTO();
        dto.setId(section.getId());
        dto.setName(section.getName());
        dto.setNoteColor(normalizeNoteColor(section.getNoteColor()));

        List<MusicItemEntity> items =
                musicItemRepository.findBySectionIdOrderByIdAsc(section.getId());
        List<MusicItemDTO> itemDTOs = new ArrayList<>();

        for (MusicItemEntity item : items) {
            MusicItemDTO itemDTO = new MusicItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setTitle(item.getTitle());
            itemDTO.setArtist(item.getArtist());
            itemDTO.setItemType(item.getItemType().name());
            itemDTO.setStatus(item.getStatus().name());
            if (item.getCatalogTrack() != null) itemDTO.setCatalogTrackId(item.getCatalogTrack().getId());
            if (item.getCatalogAlbum() != null) itemDTO.setCatalogAlbumId(item.getCatalogAlbum().getId());
            itemDTOs.add(itemDTO);
        }

        dto.setItems(itemDTOs);
        return dto;
    }

    private String normalizeNoteColor(String noteColor) {
        if (noteColor != null && List.of("CREAM", "ROSE", "PEACH", "MINT", "SKY", "LAVENDER")
                .contains(noteColor)) {
            return noteColor;
        }
        return "CREAM";
    }
}
