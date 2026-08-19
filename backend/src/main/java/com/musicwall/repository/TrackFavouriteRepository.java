package com.musicwall.repository;
import com.musicwall.entity.TrackFavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TrackFavouriteRepository extends JpaRepository<TrackFavouriteEntity, Long> {
    Optional<TrackFavouriteEntity> findByUserUsernameAndTrackId(String username, Long trackId);
    List<TrackFavouriteEntity> findByUserUsernameOrderByCreatedAtDesc(String username);
    boolean existsByUserUsernameAndTrackId(String username, Long trackId);
}
