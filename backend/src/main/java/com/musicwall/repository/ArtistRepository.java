package com.musicwall.repository;

import com.musicwall.entity.ArtistEntity;
import com.musicwall.dto.CatalogSuggestionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<ArtistEntity> findByMusicBrainzId(String musicBrainzId);

    Optional<ArtistEntity> findFirstByNameIgnoreCase(String name);

    List<ArtistEntity> findAllByOrderByNameAsc();

    @Query(value = """
            SELECT a.*
            FROM artist a
            WHERE lower(a.name) LIKE '%' || lower(:query) || '%'
               OR lower(a.name) % lower(:query)
            ORDER BY
                CASE
                    WHEN lower(a.name) = lower(:query) THEN 3
                    WHEN lower(a.name) LIKE lower(:query) || '%' THEN 2
                    WHEN lower(a.name) LIKE '%' || lower(:query) || '%' THEN 1
                    ELSE 0
                END DESC,
                similarity(lower(a.name), lower(:query)) DESC,
                a.name ASC
            """, nativeQuery = true)
    List<ArtistEntity> searchSimilar(@Param("query") String query);

    @Query(value = """
            SELECT a.id AS id,
                   'ARTIST' AS type,
                   a.name AS title,
                   'Artist' AS subtitle,
                   CAST(GREATEST(
                       similarity(lower(a.name), lower(:query)),
                       CASE
                           WHEN lower(a.name) = lower(:query) THEN 1.50
                           WHEN lower(a.name) LIKE lower(:query) || '%' THEN 1.20
                           WHEN lower(a.name) LIKE '%' || lower(:query) || '%' THEN 1.00
                           ELSE 0.00
                       END
                   ) AS real) AS score
            FROM artist a
            WHERE lower(a.name) LIKE '%' || lower(:query) || '%'
               OR lower(a.name) % lower(:query)
            ORDER BY score DESC, a.name ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<CatalogSuggestionProjection> findSuggestions(
            @Param("query") String query,
            @Param("limit") int limit
    );
}
