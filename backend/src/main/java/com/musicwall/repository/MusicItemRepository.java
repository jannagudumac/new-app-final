package com.musicwall.repository;

import com.musicwall.entity.MusicItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MusicItemRepository extends JpaRepository<MusicItemEntity, Long> {

    List<MusicItemEntity> findBySectionIdOrderByIdAsc(Long sectionId);

    void deleteBySectionId(Long sectionId);
}
