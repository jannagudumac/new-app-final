package com.musicwall.service;

import com.musicwall.dto.CreateMusicItemRequest;
import com.musicwall.dto.MusicItemDTO;
import com.musicwall.entity.ListeningStatus;
import com.musicwall.entity.MusicItemEntity;
import com.musicwall.entity.MusicItemType;
import com.musicwall.entity.MusicSectionEntity;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.exception.BusinessException;
import com.musicwall.repository.MusicItemRepository;
import com.musicwall.repository.MusicSectionRepository;
import com.musicwall.repository.MusicWallRepository;
import com.musicwall.repository.TrackRepository;
import com.musicwall.repository.AlbumRepository;
import org.springframework.stereotype.Service;

@Service
public class MusicItemService {

    private final MusicItemRepository musicItemRepository;
    private final MusicSectionRepository musicSectionRepository;
    private final WallAccessService wallAccessService;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;

    public MusicItemService(
            MusicItemRepository musicItemRepository,
            MusicSectionRepository musicSectionRepository,
            WallAccessService wallAccessService,
            TrackRepository trackRepository,
            AlbumRepository albumRepository
    ) {
        this.musicItemRepository = musicItemRepository;
        this.musicSectionRepository = musicSectionRepository;
        this.wallAccessService = wallAccessService;
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
    }

    public MusicItemDTO createItem(
            String username,
            Long wallId,
            Long sectionId,
            CreateMusicItemRequest request
    ) {
        verifyWallAccess(username, wallId);
        MusicSectionEntity section = findSectionInWall(sectionId, wallId);

        MusicItemEntity item = new MusicItemEntity();
        applyRequest(item, request);
        item.setSection(section);

        return convertToDTO(musicItemRepository.save(item));
    }

    public MusicItemDTO updateItem(
            String username,
            Long wallId,
            Long sectionId,
            Long itemId,
            CreateMusicItemRequest request
    ) {
        verifyWallAccess(username, wallId);
        findSectionInWall(sectionId, wallId);
        MusicItemEntity item = findItemInSection(itemId, sectionId);
        applyRequest(item, request);
        return convertToDTO(musicItemRepository.save(item));
    }

    public void deleteItem(
            String username,
            Long wallId,
            Long sectionId,
            Long itemId
    ) {
        verifyWallAccess(username, wallId);
        findSectionInWall(sectionId, wallId);
        MusicItemEntity item = findItemInSection(itemId, sectionId);
        musicItemRepository.delete(item);
    }

    private void verifyWallAccess(String username, Long wallId) {
        wallAccessService.findAccessibleWall(username, wallId);
    }

    private MusicSectionEntity findSectionInWall(Long sectionId, Long wallId) {
        MusicSectionEntity section = musicSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (!section.getWall().getId().equals(wallId)) {
            throw new ResourceNotFoundException("Section not found");
        }

        return section;
    }

    private MusicItemEntity findItemInSection(Long itemId, Long sectionId) {
        MusicItemEntity item = musicItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Music item not found"));

        if (!item.getSection().getId().equals(sectionId)) {
            throw new ResourceNotFoundException("Music item not found");
        }

        return item;
    }

    private void applyRequest(MusicItemEntity item, CreateMusicItemRequest request) {
        item.setCatalogTrack(null);
        item.setCatalogAlbum(null);
        if (request.getCatalogTrackId() != null) {
            var track = trackRepository.findById(request.getCatalogTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Track not found"));
            item.setCatalogTrack(track);
            item.setTitle(track.getTitle());
            item.setArtist(track.getArtist().getName());
            item.setItemType(MusicItemType.TRACK);
        } else if (request.getCatalogAlbumId() != null) {
            var album = albumRepository.findById(request.getCatalogAlbumId())
                    .orElseThrow(() -> new ResourceNotFoundException("Album not found"));
            item.setCatalogAlbum(album);
            item.setTitle(album.getTitle());
            item.setArtist(album.getArtist().getName());
            item.setItemType(MusicItemType.ALBUM);
        } else {
            throw new BusinessException("Choose a track or album from the catalogue");
        }
        item.setStatus(ListeningStatus.valueOf(request.getStatus()));
    }

    private MusicItemDTO convertToDTO(MusicItemEntity item) {
        MusicItemDTO dto = new MusicItemDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setArtist(item.getArtist());
        dto.setItemType(item.getItemType().name());
        dto.setStatus(item.getStatus().name());
        if (item.getCatalogTrack() != null) dto.setCatalogTrackId(item.getCatalogTrack().getId());
        if (item.getCatalogAlbum() != null) dto.setCatalogAlbumId(item.getCatalogAlbum().getId());
        return dto;
    }
}
