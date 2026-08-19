package com.musicwall.repository;

import com.musicwall.entity.TrackEntity;
import com.musicwall.dto.CatalogSuggestionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<TrackEntity, Long> {

    long countByArtistId(Long artistId);

    boolean existsByArtistIdAndTitleIgnoreCase(Long artistId, String title);

    Optional<TrackEntity> findByMusicBrainzId(String musicBrainzId);

    Optional<TrackEntity> findFirstByArtistIdAndTitleIgnoreCase(Long artistId, String title);

    List<TrackEntity> findAllByOrderByTitleAsc();

    List<TrackEntity> findByArtistIdOrderByTitleAsc(Long artistId);

    List<TrackEntity> findByAlbumIdOrderByTitleAsc(Long albumId);

    @Query(value = """
            SELECT t.*
            FROM track t
            JOIN artist ar ON ar.id = t.artist_id
            WHERE lower(t.title) LIKE '%' || lower(:query) || '%'
               OR lower(ar.name) LIKE '%' || lower(:query) || '%'
               OR lower(t.title) % lower(:query)
               OR lower(ar.name) % lower(:query)
            ORDER BY GREATEST(
                similarity(lower(t.title), lower(:query)),
                similarity(lower(ar.name), lower(:query))
            ) DESC,
            t.title ASC
            """, nativeQuery = true)
    List<TrackEntity> searchSimilar(@Param("query") String query);

    @Query(value = """
            SELECT t.id AS id,
                   'TRACK' AS type,
                   t.title AS title,
                   ar.name AS subtitle,
                   CAST(GREATEST(
                       similarity(lower(t.title), lower(:query)),
                       similarity(lower(ar.name), lower(:query)),
                       CASE
                           WHEN lower(t.title) = lower(:query) THEN 1.50
                           WHEN lower(t.title) LIKE lower(:query) || '%' THEN 1.20
                           WHEN lower(t.title) LIKE '%' || lower(:query) || '%' THEN 1.00
                           ELSE 0.00
                       END
                   ) AS real) AS score
            FROM track t
            JOIN artist ar ON ar.id = t.artist_id
            WHERE lower(t.title) LIKE '%' || lower(:query) || '%'
               OR lower(ar.name) LIKE '%' || lower(:query) || '%'
               OR lower(t.title) % lower(:query)
               OR lower(ar.name) % lower(:query)
            ORDER BY score DESC, t.title ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<CatalogSuggestionProjection> findSuggestions(
            @Param("query") String query,
            @Param("limit") int limit
    );
}
