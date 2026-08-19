package com.musicwall.controller;

import com.musicwall.dto.FriendDTO;
import com.musicwall.dto.FriendRequestDTO;
import com.musicwall.dto.UserSearchDTO;
import com.musicwall.service.FriendshipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendDTO>> friends(Authentication authentication) {
        return ResponseEntity.ok(friendshipService.getFriends(authentication.getName()));
    }

    @GetMapping("/friends/search")
    public ResponseEntity<List<UserSearchDTO>> search(
            Authentication authentication,
            @RequestParam String query
    ) {
        return ResponseEntity.ok(friendshipService.searchUsers(authentication.getName(), query));
    }

    @GetMapping("/friend-requests")
    public ResponseEntity<List<FriendRequestDTO>> requests(Authentication authentication) {
        return ResponseEntity.ok(friendshipService.getIncomingRequests(authentication.getName()));
    }

    @PostMapping("/friend-requests/{username}")
    public ResponseEntity<FriendRequestDTO> send(
            Authentication authentication,
            @PathVariable String username
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.sendRequest(authentication.getName(), username));
    }

    @PostMapping("/friend-requests/{id}/accept")
    public ResponseEntity<Void> accept(
            Authentication authentication,
            @PathVariable Long id
    ) {
        friendshipService.answerRequest(authentication.getName(), id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/friend-requests/{id}/reject")
    public ResponseEntity<Void> reject(
            Authentication authentication,
            @PathVariable Long id
    ) {
        friendshipService.answerRequest(authentication.getName(), id, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/friends/{username}")
    public ResponseEntity<Void> remove(
            Authentication authentication,
            @PathVariable String username
    ) {
        friendshipService.removeFriend(authentication.getName(), username);
        return ResponseEntity.noContent().build();
    }
}
