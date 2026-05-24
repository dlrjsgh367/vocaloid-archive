package com.vocaloidarchive.playlist.application.port;

import com.vocaloidarchive.playlist.domain.Playlist;
import java.util.Optional;

public interface PlaylistRepository {
  Playlist save(Playlist playlist);
  Optional<Playlist> findById(Long id);
  void deleteById(Long id);
  boolean existsById(Long id);

  boolean existsByShareCode(String shareCode);

  /** Persists a share code on an existing playlist (flushes so unique violations surface eagerly). */
  void updateShareCode(Long playlistId, String shareCode);
}
