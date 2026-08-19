package com.musicwall.controller;

import com.musicwall.dto.CreateMusicItemRequest;
import com.musicwall.dto.MusicItemDTO;
import com.musicwall.service.MusicItemService;
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
@RequestMapping("/api/walls/{wallId}/sections/{sectionId}/items")
public class MusicItemController {

    private final MusicItemService musicItemService;

    public MusicItemController(MusicItemService musicItemService) {
        this.musicItemService = musicItemService;
    }

    @PostMapping
    public ResponseEntity<MusicItemDTO> createItem(
            @PathVariable Long wallId,
            @PathVariable Long sectionId,
            Authentication authentication,
            @Valid @RequestBody CreateMusicItemRequest request
    ) {
        return ResponseEntity.ok(musicItemService.createItem(
                authentication.getName(),
                wallId,
                sectionId,
                request
        ));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<MusicItemDTO> updateItem(
            @PathVariable Long wallId,
            @PathVariable Long sectionId,
            @PathVariable Long itemId,
            Authentication authentication,
            @Valid @RequestBody CreateMusicItemRequest request
    ) {
        return ResponseEntity.ok(musicItemService.updateItem(
                authentication.getName(),
                wallId,
                sectionId,
                itemId,
                request
        ));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long wallId,
            @PathVariable Long sectionId,
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        musicItemService.deleteItem(
                authentication.getName(),
                wallId,
                sectionId,
                itemId
        );
        return ResponseEntity.noContent().build();
    }
}
