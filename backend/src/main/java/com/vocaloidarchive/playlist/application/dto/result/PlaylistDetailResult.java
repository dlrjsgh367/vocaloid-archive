package com.vocaloidarchive.playlist.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDetailResult(
    Long id, String title, boolean isPublic, String ownerUsername,
    List<SongItem> songs, LocalDateTime createdAt) {
  public record SongItem(Long songId, String title, String thumbnailUrl, int orderIndex) {}
}
