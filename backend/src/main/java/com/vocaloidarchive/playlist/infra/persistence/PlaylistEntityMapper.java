package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.playlist.domain.Playlist;

final class PlaylistEntityMapper {
  private PlaylistEntityMapper() {}

  static Playlist toDomain(PlaylistEntity e) {
    return e == null ? null : Playlist.reconstitute(
        e.getId(), e.getUser().getId(), e.getTitle(), e.isPublic(), e.getShareCode(),
        e.getCreatedAt());
  }
}
