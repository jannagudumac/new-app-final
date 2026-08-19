package com.musicwall.repository;

import com.musicwall.entity.ArtistFavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ArtistFavouriteRepository extends JpaRepository<ArtistFavouriteEntity, Long> {
    Optional<ArtistFavouriteEntity> findByUserUsernameAndArtistId(String username, Long artistId);
    List<ArtistFavouriteEntity> findByUserUsernameOrderByCreatedAtDesc(String username);
    boolean existsByUserUsernameAndArtistId(String username, Long artistId);
}
