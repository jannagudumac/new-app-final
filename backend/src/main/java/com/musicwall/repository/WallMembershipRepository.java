package com.musicwall.repository;
import com.musicwall.entity.WallMembershipEntity;
import com.musicwall.entity.WallRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface WallMembershipRepository extends JpaRepository<WallMembershipEntity, Long> {
    Optional<WallMembershipEntity> findByWallIdAndUserUsername(Long wallId, String username);
    boolean existsByWallIdAndUserUsername(Long wallId, String username);
    List<WallMembershipEntity> findByUserUsernameOrderByWallIdDesc(String username);
    List<WallMembershipEntity> findByWallIdOrderByJoinedAtAsc(Long wallId);
    void deleteByWallId(Long wallId);
}
