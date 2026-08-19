package com.musicwall.repository;

import com.musicwall.entity.FriendshipEntity;
import com.musicwall.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {

    @Query("""
            select friendship from FriendshipEntity friendship
            where (friendship.requester.username = :first and friendship.addressee.username = :second)
               or (friendship.requester.username = :second and friendship.addressee.username = :first)
            """)
    Optional<FriendshipEntity> findBetween(
            @Param("first") String first,
            @Param("second") String second
    );

    @Query("""
            select friendship from FriendshipEntity friendship
            where friendship.status = :status
              and (friendship.requester.username = :username or friendship.addressee.username = :username)
            order by friendship.createdAt desc
            """)
    List<FriendshipEntity> findForUserWithStatus(
            @Param("username") String username,
            @Param("status") FriendshipStatus status
    );

    List<FriendshipEntity> findByAddresseeUsernameAndStatusOrderByCreatedAtDesc(
            String username,
            FriendshipStatus status
    );

    Optional<FriendshipEntity> findByIdAndAddresseeUsername(Long id, String username);
}
