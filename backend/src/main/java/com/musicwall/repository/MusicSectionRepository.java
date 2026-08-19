package com.musicwall.repository;

import com.musicwall.entity.MusicSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MusicSectionRepository extends JpaRepository<MusicSectionEntity, Long> {

    List<MusicSectionEntity> findByWallIdOrderByIdAsc(Long wallId);

    void deleteByWallId(Long wallId);
}
