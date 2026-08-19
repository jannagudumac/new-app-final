package com.musicwall.repository;

import com.musicwall.entity.AlbumFavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlbumFavouriteRepository extends JpaRepository<AlbumFavouriteEntity, Long> {
    Optional<AlbumFavouriteEntity> findByUserUsernameAndAlbumId(String username, Long albumId);
    List<AlbumFavouriteEntity> findByUserUsernameOrderByCreatedAtDesc(String username);
    boolean existsByUserUsernameAndAlbumId(String username, Long albumId);
}
