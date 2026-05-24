ALTER TABLE playlists
  ADD COLUMN share_code VARCHAR(10) NULL UNIQUE AFTER is_public;
