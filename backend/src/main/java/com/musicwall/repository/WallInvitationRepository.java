package com.musicwall.repository;
import com.musicwall.entity.InvitationStatus;
import com.musicwall.entity.WallInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface WallInvitationRepository extends JpaRepository<WallInvitationEntity, Long> {
    boolean existsByWallIdAndInvitedUserIdAndStatus(Long wallId, Long userId, InvitationStatus status);
    List<WallInvitationEntity> findByInvitedUserUsernameAndStatusOrderByCreatedAtDesc(String username, InvitationStatus status);
    Optional<WallInvitationEntity> findByIdAndInvitedUserUsername(Long id, String username);
    void deleteByWallId(Long wallId);

    @Modifying
    @Query("""
            update WallInvitationEntity invitation
               set invitation.status = com.musicwall.entity.InvitationStatus.REJECTED
             where invitation.status = com.musicwall.entity.InvitationStatus.PENDING
               and ((invitation.invitedBy.username = :firstUsername
                     and invitation.invitedUser.username = :secondUsername)
                 or (invitation.invitedBy.username = :secondUsername
                     and invitation.invitedUser.username = :firstUsername))
            """)
    int rejectPendingBetween(
            @Param("firstUsername") String firstUsername,
            @Param("secondUsername") String secondUsername
    );
}
