package com.musicwall.repository;

import com.musicwall.entity.MusicWallEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicWallRepository extends JpaRepository<MusicWallEntity, Long> {

    List<MusicWallEntity> findByOwnerUsernameOrderByIdDesc(String username);

    Optional<MusicWallEntity> findByIdAndOwnerUsername(Long id, String username);
}
