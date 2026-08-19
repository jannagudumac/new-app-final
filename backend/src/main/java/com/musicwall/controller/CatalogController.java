package com.musicwall.controller;

import com.musicwall.dto.AlbumDTO;
import com.musicwall.dto.ArtistDTO;
import com.musicwall.dto.ArtistDetailDTO;
import com.musicwall.dto.CatalogSearchDTO;
import com.musicwall.dto.CatalogSuggestionDTO;
import com.musicwall.dto.TrackDTO;
import com.musicwall.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/search")
    public ResponseEntity<CatalogSearchDTO> search(
            @RequestParam(defaultValue = "") String query
    ) {
        return ResponseEntity.ok(catalogService.search(query));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<CatalogSuggestionDTO>> suggestions(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(catalogService.getSuggestions(query));
    }

    @GetMapping("/artists/{id}")
    public ResponseEntity<ArtistDetailDTO> getArtist(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getArtist(id));
    }

    @GetMapping("/albums/{id}")
    public ResponseEntity<AlbumDTO> getAlbum(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getAlbum(id));
    }

    @GetMapping("/tracks/{id}")
    public ResponseEntity<TrackDTO> getTrack(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getTrack(id));
    }

}
