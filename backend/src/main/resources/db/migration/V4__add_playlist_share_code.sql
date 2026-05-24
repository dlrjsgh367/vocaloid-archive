ALTER TABLE playlists
  ADD COLUMN share_code CHAR(10) NULL UNIQUE AFTER is_public;
