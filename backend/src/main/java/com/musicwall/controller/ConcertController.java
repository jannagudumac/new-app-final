package com.musicwall.controller;
import com.musicwall.dto.ConcertDTO;
import com.musicwall.service.ConcertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/concerts")
public class ConcertController {
    private final ConcertService service;
    public ConcertController(ConcertService service) { this.service = service; }
    @GetMapping
    public ResponseEntity<List<ConcertDTO>> search(@RequestParam String artist, @RequestParam(required = false) String city) {
        return ResponseEntity.ok(service.search(artist, city));
    }
}
