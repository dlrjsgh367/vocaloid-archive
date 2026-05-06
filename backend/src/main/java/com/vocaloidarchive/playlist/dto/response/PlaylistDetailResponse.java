package com.vocaloidarchive.playlist.dto.response;

import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDetailResponse(
    Long id,
    String title,
    boolean isPublic,
    String ownerUsername,
    List<SongItem> songs,
    LocalDateTime createdAt
) {
  public record SongItem(Long songId, String title, String thumbnailUrl, int orderIndex) {}

  public static PlaylistDetailResponse from(Playlist p, List<PlaylistSong> songs) {
    List<SongItem> items = songs.stream()
        .map(ps -> new SongItem(ps.getSong().getId(), ps.getSong().getTitle(),
            ps.getSong().getThumbnailUrl(), ps.getOrderIndex()))
        .toList();
    return new PlaylistDetailResponse(p.getId(), p.getTitle(), p.isPublic(),
        p.getUser().getUsername(), items, p.getCreatedAt());
  }
}
