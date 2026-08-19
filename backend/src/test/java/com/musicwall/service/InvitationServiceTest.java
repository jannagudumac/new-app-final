package com.musicwall.service;

import com.musicwall.dto.InvitationRequest;
import com.musicwall.entity.InvitationStatus;
import com.musicwall.entity.MusicWallEntity;
import com.musicwall.entity.UserEntity;
import com.musicwall.entity.WallInvitationEntity;
import com.musicwall.entity.WallMembershipEntity;
import com.musicwall.entity.WallRole;
import com.musicwall.exception.ForbiddenException;
import com.musicwall.repository.UserRepository;
import com.musicwall.repository.WallInvitationRepository;
import com.musicwall.repository.WallMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    WallAccessService accessService;

    @Mock
    UserRepository userRepository;

    @Mock
    WallInvitationRepository invitationRepository;

    @Mock
    WallMembershipRepository membershipRepository;

    @Mock
    FriendshipService friendshipService;

    InvitationService service;

    @BeforeEach
    void setUp() {
        service = new InvitationService(
                accessService,
                userRepository,
                invitationRepository,
                membershipRepository,
                friendshipService
        );
    }

    @Test
    void onlyTheOwnerCanSendAnInvitation() {
        InvitationRequest request = new InvitationRequest();
        request.setUsername("alice");
        when(accessService.findOwnedWall("janna", 12L))
                .thenThrow(new ForbiddenException("Only the owner can invite people"));

        assertThrows(ForbiddenException.class, () -> service.invite("janna", 12L, request));

        verify(userRepository, never()).findByUsername(any());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void ownerCanInviteAnExistingNonMember() {
        UserEntity owner = user(1L, "janna");
        UserEntity invited = user(2L, "alice");
        MusicWallEntity wall = wall(12L, "Jazz discoveries", owner);
        InvitationRequest request = new InvitationRequest();
        request.setUsername(" alice ");

        when(accessService.findOwnedWall("janna", 12L)).thenReturn(wall);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(invited));
        when(friendshipService.areFriends("janna", "alice")).thenReturn(true);
        when(userRepository.findByUsername("janna")).thenReturn(Optional.of(owner));
        when(invitationRepository.save(any(WallInvitationEntity.class))).thenAnswer(invocation -> {
            WallInvitationEntity invitation = invocation.getArgument(0);
            invitation.setId(25L);
            return invitation;
        });

        var result = service.invite("janna", 12L, request);

        assertEquals(25L, result.getId());
        assertEquals("Jazz discoveries", result.getWallName());
        assertEquals("janna", result.getInvitedByUsername());
        assertEquals("PENDING", result.getStatus());
        verify(membershipRepository).existsByWallIdAndUserUsername(12L, "alice");
        verify(invitationRepository).existsByWallIdAndInvitedUserIdAndStatus(
                12L,
                2L,
                InvitationStatus.PENDING
        );
    }

    @Test
    void ownerCannotInviteSomeoneWhoIsNotAFriend() {
        UserEntity owner = user(1L, "janna");
        UserEntity invited = user(2L, "alice");
        MusicWallEntity wall = wall(12L, "Jazz discoveries", owner);
        InvitationRequest request = new InvitationRequest();
        request.setUsername("alice");

        when(accessService.findOwnedWall("janna", 12L)).thenReturn(wall);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(invited));
        when(friendshipService.areFriends("janna", "alice")).thenReturn(false);

        assertThrows(
                com.musicwall.exception.BusinessException.class,
                () -> service.invite("janna", 12L, request)
        );

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void acceptingAnInvitationCreatesTheMembership() {
        UserEntity invited = user(2L, "alice");
        UserEntity owner = user(1L, "janna");
        MusicWallEntity wall = wall(12L, "Jazz discoveries", owner);
        WallInvitationEntity invitation = new WallInvitationEntity();
        invitation.setId(25L);
        invitation.setWall(wall);
        invitation.setInvitedUser(invited);
        invitation.setInvitedBy(owner);

        when(invitationRepository.findByIdAndInvitedUserUsername(25L, "alice"))
                .thenReturn(Optional.of(invitation));
        when(friendshipService.areFriends("janna", "alice")).thenReturn(true);

        service.answer("alice", 25L, true);

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        ArgumentCaptor<WallMembershipEntity> membershipCaptor =
                ArgumentCaptor.forClass(WallMembershipEntity.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertEquals(wall, membershipCaptor.getValue().getWall());
        assertEquals(invited, membershipCaptor.getValue().getUser());
        assertEquals(WallRole.MEMBER, membershipCaptor.getValue().getRole());
    }

    @Test
    void invitationCannotBeAcceptedAfterFriendshipWasRemoved() {
        UserEntity invited = user(2L, "roby");
        UserEntity owner = user(1L, "janna");
        MusicWallEntity wall = wall(12L, "Jazz discoveries", owner);
        WallInvitationEntity invitation = new WallInvitationEntity();
        invitation.setId(25L);
        invitation.setWall(wall);
        invitation.setInvitedUser(invited);
        invitation.setInvitedBy(owner);

        when(invitationRepository.findByIdAndInvitedUserUsername(25L, "roby"))
                .thenReturn(Optional.of(invitation));
        when(friendshipService.areFriends("janna", "roby")).thenReturn(false);

        assertThrows(
                com.musicwall.exception.BusinessException.class,
                () -> service.answer("roby", 25L, true)
        );

        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        verify(invitationRepository, never()).save(any());
        verify(membershipRepository, never()).save(any());
    }

    private UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private MusicWallEntity wall(Long id, String name, UserEntity owner) {
        MusicWallEntity wall = new MusicWallEntity();
        wall.setId(id);
        wall.setName(name);
        wall.setOwner(owner);
        return wall;
    }
}
