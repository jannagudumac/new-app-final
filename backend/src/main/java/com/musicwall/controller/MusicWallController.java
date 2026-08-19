package com.musicwall.controller;

import com.musicwall.dto.CreateMusicWallRequest;
import com.musicwall.dto.MusicWallDTO;
import com.musicwall.dto.MusicWallDetailDTO;
import com.musicwall.dto.UpdateWallAppearanceRequest;
import com.musicwall.service.MusicWallService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/walls")
public class MusicWallController {

    private final MusicWallService musicWallService;

    public MusicWallController(MusicWallService musicWallService) {
        this.musicWallService = musicWallService;
    }

    @PostMapping
    public ResponseEntity<MusicWallDTO> createWall(
            Authentication authentication,
            @Valid @RequestBody CreateMusicWallRequest request
    ) {
        MusicWallDTO wall = musicWallService.createWall(
                authentication.getName(),
                request
        );
        return ResponseEntity.ok(wall);
    }

    @GetMapping
    public ResponseEntity<List<MusicWallDTO>> getMyWalls(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                musicWallService.getWallsForUser(authentication.getName())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicWallDetailDTO> getWall(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                musicWallService.getWall(id, authentication.getName())
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicWallDTO> updateWall(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CreateMusicWallRequest request
    ) {
        return ResponseEntity.ok(musicWallService.updateWall(
                id,
                authentication.getName(),
                request
        ));
    }

    @PutMapping("/{id}/appearance")
    public ResponseEntity<MusicWallDTO> updateWallAppearance(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UpdateWallAppearanceRequest request
    ) {
        return ResponseEntity.ok(musicWallService.updateWallAppearance(
                id,
                authentication.getName(),
                request
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWall(
            @PathVariable Long id,
            Authentication authentication
    ) {
        musicWallService.deleteWall(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
