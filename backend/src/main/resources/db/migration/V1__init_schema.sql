-- ============================================================
-- VocaloidArchive Schema V1
-- ============================================================

CREATE TABLE users (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  username           VARCHAR(50)  NOT NULL,
  email              VARCHAR(100) NOT NULL,
  password_hash      VARCHAR(255) NOT NULL,
  profile_image_url  VARCHAR(500) NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  TIMESTAMP    NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_refresh_tokens_user_id (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE characters (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(100) NOT NULL,
  color_hex  VARCHAR(7)   NOT NULL,
  image_url  VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_characters_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tags (
  id    BIGINT      NOT NULL AUTO_INCREMENT,
  name  VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE songs (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  registered_by   BIGINT       NOT NULL,
  title           VARCHAR(200) NOT NULL,
  youtube_url     VARCHAR(500) NULL,
  niconico_url    VARCHAR(500) NULL,
  thumbnail_url   VARCHAR(500) NULL,
  bpm             INT          NULL,
  mood            VARCHAR(20)  NOT NULL,
  play_count      INT          NOT NULL DEFAULT 0,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_songs_created_at (created_at),
  KEY ix_songs_play_count (play_count),
  CONSTRAINT fk_songs_registered_by FOREIGN KEY (registered_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE song_characters (
  song_id      BIGINT NOT NULL,
  character_id BIGINT NOT NULL,
  PRIMARY KEY (song_id, character_id),
  KEY ix_song_characters_character_id (character_id),
  CONSTRAINT fk_song_characters_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
  CONSTRAINT fk_song_characters_character FOREIGN KEY (character_id) REFERENCES characters (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE song_tags (
  song_id BIGINT NOT NULL,
  tag_id  BIGINT NOT NULL,
  PRIMARY KEY (song_id, tag_id),
  KEY ix_song_tags_tag_id (tag_id),
  CONSTRAINT fk_song_tags_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
  CONSTRAINT fk_song_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlists (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  title       VARCHAR(200) NOT NULL,
  is_public   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_playlists_user_id (user_id),
  CONSTRAINT fk_playlists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_songs (
  playlist_id  BIGINT NOT NULL,
  song_id      BIGINT NOT NULL,
  order_index  INT    NOT NULL,
  PRIMARY KEY (playlist_id, song_id),
  KEY ix_playlist_songs_order (playlist_id, order_index),
  CONSTRAINT fk_playlist_songs_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
  CONSTRAINT fk_playlist_songs_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE likes (
  user_id   BIGINT    NOT NULL,
  song_id   BIGINT    NOT NULL,
  liked_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, song_id),
  KEY ix_likes_song_id (song_id),
  CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_likes_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  song_id     BIGINT       NOT NULL,
  content     VARCHAR(500) NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_comments_song_created (song_id, created_at),
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
