package com.musicwall.service;

import com.musicwall.dto.CreateMusicSectionRequest;
import com.musicwall.dto.MusicSectionDTO;
import com.musicwall.entity.MusicSectionEntity;
import com.musicwall.entity.MusicWallEntity;
import com.musicwall.repository.MusicItemRepository;
import com.musicwall.repository.MusicSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class MusicSectionServiceTest {

    @Mock
    private MusicSectionRepository musicSectionRepository;

    @Mock
    private MusicItemRepository musicItemRepository;

    @Mock
    private WallAccessService wallAccessService;

    private MusicSectionService musicSectionService;

    @BeforeEach
    void setUp() {
        musicSectionService = new MusicSectionService(
                musicSectionRepository,
                musicItemRepository,
                wallAccessService
        );
    }

    @Test
    void createSectionKeepsSelectedNoteColor() {
        MusicWallEntity wall = new MusicWallEntity();
        wall.setId(5L);

        CreateMusicSectionRequest request = new CreateMusicSectionRequest();
        request.setName("Jazz");
        request.setNoteColor("MINT");

        when(wallAccessService.findAccessibleWall("janna", 5L)).thenReturn(wall);
        when(musicSectionRepository.save(any(MusicSectionEntity.class))).thenAnswer(invocation -> {
            MusicSectionEntity section = invocation.getArgument(0);
            section.setId(12L);
            return section;
        });

        MusicSectionDTO result = musicSectionService.createSection("janna", 5L, request);

        assertEquals("MINT", result.getNoteColor());
        assertEquals("Jazz", result.getName());
    }

    @Test
    void oldSectionWithoutNoteColorUsesCream() {
        MusicWallEntity wall = new MusicWallEntity();
        wall.setId(5L);

        MusicSectionEntity oldSection = new MusicSectionEntity();
        oldSection.setId(12L);
        oldSection.setName("Jazz");
        oldSection.setNoteColor(null);

        when(wallAccessService.findAccessibleWall("janna", 5L)).thenReturn(wall);
        when(musicSectionRepository.findByWallIdOrderByIdAsc(5L))
                .thenReturn(List.of(oldSection));
        when(musicItemRepository.findBySectionIdOrderByIdAsc(12L))
                .thenReturn(List.of());

        List<MusicSectionDTO> result =
                musicSectionService.getSectionsForWall("janna", 5L);

        assertEquals("CREAM", result.get(0).getNoteColor());
    }
}
