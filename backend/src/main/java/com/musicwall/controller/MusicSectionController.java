package com.musicwall.controller;

import com.musicwall.dto.CreateMusicSectionRequest;
import com.musicwall.dto.MusicSectionDTO;
import com.musicwall.service.MusicSectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/walls/{wallId}/sections")
public class MusicSectionController {

    private final MusicSectionService musicSectionService;

    public MusicSectionController(MusicSectionService musicSectionService) {
        this.musicSectionService = musicSectionService;
    }

    @PostMapping
    public ResponseEntity<MusicSectionDTO> createSection(
            @PathVariable Long wallId,
            Authentication authentication,
            @Valid @RequestBody CreateMusicSectionRequest request
    ) {
        return ResponseEntity.ok(musicSectionService.createSection(
                authentication.getName(),
                wallId,
                request
        ));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<MusicSectionDTO> updateSection(
            @PathVariable Long wallId,
            @PathVariable Long sectionId,
            Authentication authentication,
            @Valid @RequestBody CreateMusicSectionRequest request
    ) {
        return ResponseEntity.ok(musicSectionService.updateSection(
                authentication.getName(),
                wallId,
                sectionId,
                request
        ));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long wallId,
            @PathVariable Long sectionId,
            Authentication authentication
    ) {
        musicSectionService.deleteSection(
                authentication.getName(),
                wallId,
                sectionId
        );
        return ResponseEntity.noContent().build();
    }
}
