package com.musicwall.dto;

/**
 * Internal view of one ranked row returned by PostgreSQL.
 */
public interface CatalogSuggestionProjection {

    Long getId();

    String getType();

    String getTitle();

    String getSubtitle();

    Float getScore();
}
