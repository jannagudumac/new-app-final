package com.musicwall.service;

import com.musicwall.entity.FriendshipEntity;
import com.musicwall.entity.FriendshipStatus;
import com.musicwall.entity.UserEntity;
import com.musicwall.exception.BusinessException;
import com.musicwall.repository.FriendshipRepository;
import com.musicwall.repository.UserRepository;
import com.musicwall.repository.WallInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    FriendshipRepository friendshipRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    WallInvitationRepository wallInvitationRepository;

    FriendshipService service;

    @BeforeEach
    void setUp() {
        service = new FriendshipService(friendshipRepository, userRepository, wallInvitationRepository);
    }

    @Test
    void sendRequestUsesTheAuthenticatedUsernameAsRequester() {
        UserEntity janna = user(1L, "janna");
        UserEntity roby = user(2L, "roby");
        when(userRepository.findByUsername("janna")).thenReturn(Optional.of(janna));
        when(userRepository.findByUsername("roby")).thenReturn(Optional.of(roby));
        when(friendshipRepository.findBetween("janna", "roby")).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(FriendshipEntity.class))).thenAnswer(invocation -> {
            FriendshipEntity friendship = invocation.getArgument(0);
            friendship.setId(10L);
            return friendship;
        });

        service.sendRequest("janna", "roby");

        ArgumentCaptor<FriendshipEntity> captor = ArgumentCaptor.forClass(FriendshipEntity.class);
        verify(friendshipRepository).save(captor.capture());
        assertEquals("janna", captor.getValue().getRequester().getUsername());
        assertEquals("roby", captor.getValue().getAddressee().getUsername());
    }

    @Test
    void onlyTheAddresseeCanAcceptARequest() {
        when(friendshipRepository.findByIdAndAddresseeUsername(10L, "janna"))
                .thenReturn(Optional.empty());

        assertThrows(
                com.musicwall.exception.ResourceNotFoundException.class,
                () -> service.answerRequest("janna", 10L, true)
        );
    }

    @Test
    void acceptedFriendshipIsReturnedForEitherSide() {
        FriendshipEntity friendship = friendship(user(1L, "janna"), user(2L, "roby"));
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findForUserWithStatus("roby", FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(friendship));

        assertEquals("janna", service.getFriends("roby").get(0).getUsername());
    }

    @Test
    void cannotSendTheSamePendingRequestTwice() {
        FriendshipEntity friendship = friendship(user(1L, "janna"), user(2L, "roby"));
        when(userRepository.findByUsername("janna")).thenReturn(Optional.of(friendship.getRequester()));
        when(userRepository.findByUsername("roby")).thenReturn(Optional.of(friendship.getAddressee()));
        when(friendshipRepository.findBetween("janna", "roby")).thenReturn(Optional.of(friendship));

        assertThrows(BusinessException.class, () -> service.sendRequest("janna", "roby"));
    }

    @Test
    void removingAFriendRejectsPendingWallInvitationsBetweenThem() {
        FriendshipEntity friendship = friendship(user(1L, "janna"), user(2L, "roby"));
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findBetween("janna", "roby"))
                .thenReturn(Optional.of(friendship));

        service.removeFriend("janna", "roby");

        verify(wallInvitationRepository).rejectPendingBetween("janna", "roby");
        verify(friendshipRepository).delete(friendship);
    }

    private UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private FriendshipEntity friendship(UserEntity requester, UserEntity addressee) {
        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        return friendship;
    }
}
