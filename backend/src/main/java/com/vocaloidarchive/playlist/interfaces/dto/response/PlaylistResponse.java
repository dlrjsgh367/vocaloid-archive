package com.vocaloidarchive.playlist.interfaces.dto.response;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import java.time.LocalDateTime;

public record PlaylistResponse(
    Long id, String title, boolean isPublic, String ownerUsername,
    long songCount, LocalDateTime createdAt) {
  public static PlaylistResponse from(PlaylistResult r) {
    return new PlaylistResponse(r.id(), r.title(), r.isPublic(),
        r.ownerUsername(), r.songCount(), r.createdAt());
  }
}
