package com.musicwall.service;

import com.musicwall.dto.FriendDTO;
import com.musicwall.dto.FriendRequestDTO;
import com.musicwall.dto.UserSearchDTO;
import com.musicwall.entity.FriendshipEntity;
import com.musicwall.entity.FriendshipStatus;
import com.musicwall.entity.UserEntity;
import com.musicwall.exception.BusinessException;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.FriendshipRepository;
import com.musicwall.repository.UserRepository;
import com.musicwall.repository.WallInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final WallInvitationRepository wallInvitationRepository;

    public FriendshipService(
            FriendshipRepository friendshipRepository,
            UserRepository userRepository,
            WallInvitationRepository wallInvitationRepository
    ) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.wallInvitationRepository = wallInvitationRepository;
    }

    public List<FriendDTO> getFriends(String username) {
        return friendshipRepository
                .findForUserWithStatus(username, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> new FriendDTO(otherUsername(friendship, username)))
                .sorted((first, second) -> first.getUsername().compareToIgnoreCase(second.getUsername()))
                .toList();
    }

    public List<FriendRequestDTO> getIncomingRequests(String username) {
        return friendshipRepository
                .findByAddresseeUsernameAndStatusOrderByCreatedAtDesc(
                        username,
                        FriendshipStatus.PENDING
                )
                .stream()
                .map(friendship -> new FriendRequestDTO(
                        friendship.getId(),
                        friendship.getRequester().getUsername()
                ))
                .toList();
    }

    public List<UserSearchDTO> searchUsers(String username, String query) {
        String cleanedQuery = query == null ? "" : query.trim();
        if (cleanedQuery.length() < 2) {
            throw new BusinessException("Enter at least two characters");
        }

        return userRepository.searchByUsername(cleanedQuery, username)
                .stream()
                .limit(20)
                .map(user -> new UserSearchDTO(
                        user.getUsername(),
                        relationshipStatus(username, user.getUsername())
                ))
                .toList();
    }

    @Transactional
    public FriendRequestDTO sendRequest(String username, String friendUsername) {
        String cleanedFriendUsername = friendUsername == null ? "" : friendUsername.trim();
        if (username.equals(cleanedFriendUsername)) {
            throw new BusinessException("You cannot add yourself as a friend");
        }

        UserEntity requester = findUser(username);
        UserEntity addressee = findUser(cleanedFriendUsername);
        Optional<FriendshipEntity> existing = friendshipRepository.findBetween(
                username,
                cleanedFriendUsername
        );

        if (existing.isPresent()) {
            FriendshipEntity friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException("This user is already your friend");
            }
            if (friendship.getRequester().getUsername().equals(username)) {
                throw new BusinessException("Friend request already sent");
            }
            throw new BusinessException("This user has already sent you a friend request");
        }

        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship = friendshipRepository.save(friendship);
        return new FriendRequestDTO(friendship.getId(), addressee.getUsername());
    }

    @Transactional
    public void answerRequest(String username, Long requestId, boolean accept) {
        FriendshipEntity friendship = friendshipRepository
                .findByIdAndAddresseeUsername(requestId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException("Friend request was already answered");
        }

        if (accept) {
            friendship.setStatus(FriendshipStatus.ACCEPTED);
            friendshipRepository.save(friendship);
        } else {
            friendshipRepository.delete(friendship);
        }
    }

    @Transactional
    public void removeFriend(String username, String friendUsername) {
        FriendshipEntity friendship = friendshipRepository
                .findBetween(username, friendUsername)
                .filter(item -> item.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResourceNotFoundException("Friend not found"));
        wallInvitationRepository.rejectPendingBetween(username, friendUsername);
        friendshipRepository.delete(friendship);
    }

    public boolean areFriends(String firstUsername, String secondUsername) {
        return friendshipRepository.findBetween(firstUsername, secondUsername)
                .map(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    private String relationshipStatus(String username, String otherUsername) {
        return friendshipRepository.findBetween(username, otherUsername)
                .map(friendship -> {
                    if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                        return "FRIEND";
                    }
                    return friendship.getRequester().getUsername().equals(username)
                            ? "PENDING_SENT"
                            : "PENDING_RECEIVED";
                })
                .orElse("NONE");
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String otherUsername(FriendshipEntity friendship, String username) {
        if (friendship.getRequester().getUsername().equals(username)) {
            return friendship.getAddressee().getUsername();
        }
        return friendship.getRequester().getUsername();
    }
}
