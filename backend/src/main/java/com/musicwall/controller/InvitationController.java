package com.musicwall.controller;
import com.musicwall.dto.*;
import com.musicwall.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InvitationController {
    private final InvitationService service;
    public InvitationController(InvitationService service) { this.service = service; }
    @PostMapping("/walls/{wallId}/invitations")
    public ResponseEntity<InvitationDTO> invite(@PathVariable Long wallId, Authentication auth, @Valid @RequestBody InvitationRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(auth.getName(), wallId, request)); }
    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationDTO>> pending(Authentication auth) { return ResponseEntity.ok(service.pending(auth.getName())); }
    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long id, Authentication auth) { service.answer(auth.getName(), id, true); return ResponseEntity.noContent().build(); }
    @PostMapping("/invitations/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, Authentication auth) { service.answer(auth.getName(), id, false); return ResponseEntity.noContent().build(); }
    @GetMapping("/walls/{wallId}/members")
    public ResponseEntity<List<WallMemberDTO>> members(@PathVariable Long wallId, Authentication auth) { return ResponseEntity.ok(service.members(auth.getName(), wallId)); }
}
