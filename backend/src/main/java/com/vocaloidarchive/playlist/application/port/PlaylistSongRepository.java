package com.vocaloidarchive.playlist.application.port;

public interface PlaylistSongRepository {
  void add(Long playlistId, Long songId, int orderIndex);
  boolean existsBy(Long playlistId, Long songId);
  void deleteBy(Long playlistId, Long songId);
  int findMaxOrderIndex(Long playlistId);
}
