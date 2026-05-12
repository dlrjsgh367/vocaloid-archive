package com.vocaloidarchive.playlist.interfaces.dto.response;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistDetailResult;
import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDetailResponse(
    Long id, String title, boolean isPublic, String ownerUsername,
    List<SongItem> songs, LocalDateTime createdAt) {
  public record SongItem(Long songId, String title, String thumbnailUrl, int orderIndex) {}

  public static PlaylistDetailResponse from(PlaylistDetailResult r) {
    return new PlaylistDetailResponse(r.id(), r.title(), r.isPublic(),
        r.ownerUsername(),
        r.songs().stream()
            .map(s -> new SongItem(s.songId(), s.title(), s.thumbnailUrl(), s.orderIndex()))
            .toList(),
        r.createdAt());
  }
}
