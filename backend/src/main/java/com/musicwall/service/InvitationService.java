package com.musicwall.service;
import com.musicwall.dto.*;
import com.musicwall.entity.*;
import com.musicwall.exception.BusinessException;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class InvitationService {
    private final WallAccessService accessService;
    private final UserRepository userRepository;
    private final WallInvitationRepository invitationRepository;
    private final WallMembershipRepository membershipRepository;
    private final FriendshipService friendshipService;
    public InvitationService(WallAccessService accessService, UserRepository userRepository,
                             WallInvitationRepository invitationRepository, WallMembershipRepository membershipRepository,
                             FriendshipService friendshipService) {
        this.accessService = accessService; this.userRepository = userRepository;
        this.invitationRepository = invitationRepository; this.membershipRepository = membershipRepository;
        this.friendshipService = friendshipService;
    }
    @Transactional
    public InvitationDTO invite(String ownerUsername, Long wallId, InvitationRequest request) {
        MusicWallEntity wall = accessService.findOwnedWall(ownerUsername, wallId);
        UserEntity invited = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (invited.getUsername().equals(ownerUsername)) throw new BusinessException("The owner is already a member");
        if (!friendshipService.areFriends(ownerUsername, invited.getUsername())) throw new BusinessException("You can invite friends only");
        if (membershipRepository.existsByWallIdAndUserUsername(wallId, invited.getUsername())) throw new BusinessException("This user is already a member");
        if (invitationRepository.existsByWallIdAndInvitedUserIdAndStatus(wallId, invited.getId(), InvitationStatus.PENDING)) throw new BusinessException("This user already has a pending invitation");
        WallInvitationEntity invitation = new WallInvitationEntity();
        invitation.setWall(wall); invitation.setInvitedUser(invited);
        invitation.setInvitedBy(userRepository.findByUsername(ownerUsername).orElseThrow());
        return convert(invitationRepository.save(invitation));
    }
    public List<InvitationDTO> pending(String username) {
        return invitationRepository.findByInvitedUserUsernameAndStatusOrderByCreatedAtDesc(username, InvitationStatus.PENDING)
                .stream()
                .filter(invitation -> friendshipService.areFriends(
                        invitation.getInvitedBy().getUsername(),
                        username
                ))
                .map(this::convert)
                .toList();
    }
    @Transactional
    public void answer(String username, Long invitationId, boolean accept) {
        WallInvitationEntity invitation = invitationRepository.findByIdAndInvitedUserUsername(invitationId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) throw new BusinessException("Invitation was already answered");
        if (accept && !friendshipService.areFriends(invitation.getInvitedBy().getUsername(), username)) {
            throw new BusinessException("You must still be friends to accept this invitation");
        }
        invitation.setStatus(accept ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED);
        invitationRepository.save(invitation);
        if (accept && !membershipRepository.existsByWallIdAndUserUsername(invitation.getWall().getId(), username)) {
            WallMembershipEntity membership = new WallMembershipEntity(); membership.setWall(invitation.getWall());
            membership.setUser(invitation.getInvitedUser()); membership.setRole(WallRole.MEMBER); membershipRepository.save(membership);
        }
    }
    public List<WallMemberDTO> members(String username, Long wallId) {
        MusicWallEntity wall = accessService.findAccessibleWall(username, wallId);
        List<WallMemberDTO> result = new java.util.ArrayList<>();
        result.add(new WallMemberDTO(
                wall.getOwner().getUsername(),
                "OWNER"
        ));
        membershipRepository.findByWallIdOrderByJoinedAtAsc(wallId).stream()
                .filter(item -> !item.getUser().getUsername().equals(wall.getOwner().getUsername()))
                .forEach(item -> result.add(new WallMemberDTO(
                        item.getUser().getUsername(),
                        item.getRole().name()
                )));
        return result;
    }
    private InvitationDTO convert(WallInvitationEntity entity) {
        InvitationDTO dto = new InvitationDTO(); dto.setId(entity.getId()); dto.setWallId(entity.getWall().getId());
        dto.setWallName(entity.getWall().getName());
        dto.setInvitedByUsername(entity.getInvitedBy().getUsername());
        dto.setStatus(entity.getStatus().name());
        return dto;
    }
}
