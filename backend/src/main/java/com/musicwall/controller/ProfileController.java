package com.musicwall.controller;
import com.musicwall.dto.FavouriteIdsDTO;
import com.musicwall.dto.ProfileDTO;
import com.musicwall.dto.ProfileAvatarDTO;
import com.musicwall.dto.UpdateProfileDTO;
import com.musicwall.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping("/profiles/{username}")
    public ResponseEntity<ProfileDTO> profile(@PathVariable String username, Authentication auth) {
        return ResponseEntity.ok(service.getProfile(username, auth == null ? "" : auth.getName()));
    }
    @PutMapping("/profiles/me")
    public ResponseEntity<ProfileDTO> updateProfile(@Valid @RequestBody UpdateProfileDTO request, Authentication auth) {
        return ResponseEntity.ok(service.updateProfile(auth.getName(), request));
    }
    @PostMapping(value = "/profiles/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileDTO> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication auth) {
        return ResponseEntity.ok(service.updateAvatar(auth.getName(), file));
    }
    @GetMapping("/profiles/{username}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable String username) {
        ProfileAvatarDTO avatar = service.getAvatar(username);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(avatar.getContentType())).body(avatar.getImage());
    }
    @GetMapping("/favourites")
    public ResponseEntity<FavouriteIdsDTO> favourites(Authentication auth) { return ResponseEntity.ok(service.getFavouriteIds(auth.getName())); }
    @PostMapping("/favourites/{type}/{itemId}")
    public ResponseEntity<Void> add(@PathVariable String type, @PathVariable Long itemId, Authentication auth) { service.addFavourite(auth.getName(), type, itemId); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/favourites/{type}/{itemId}")
    public ResponseEntity<Void> remove(@PathVariable String type, @PathVariable Long itemId, Authentication auth) { service.removeFavourite(auth.getName(), type, itemId); return ResponseEntity.noContent().build(); }
    @GetMapping("/favourites/{type}/{itemId}")
    public ResponseEntity<Map<String, Boolean>> check(@PathVariable String type, @PathVariable Long itemId, Authentication auth) { return ResponseEntity.ok(Map.of("favourite", service.isFavourite(auth.getName(), type, itemId))); }
}
