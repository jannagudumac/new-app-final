CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_artist_name_trgm
    ON artist USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_album_title_trgm
    ON album USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_track_title_trgm
    ON track USING gin (lower(title) gin_trgm_ops);
