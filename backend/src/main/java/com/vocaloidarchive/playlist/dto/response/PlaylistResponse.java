package com.vocaloidarchive.playlist.dto.response;

import com.vocaloidarchive.playlist.domain.Playlist;

import java.time.LocalDateTime;

public record PlaylistResponse(
    Long id,
    String title,
    boolean isPublic,
    String ownerUsername,
    long songCount,
    LocalDateTime createdAt
) {
  public static PlaylistResponse from(Playlist p, long songCount) {
    return new PlaylistResponse(p.getId(), p.getTitle(), p.isPublic(),
        p.getUser().getUsername(), songCount, p.getCreatedAt());
  }
}
