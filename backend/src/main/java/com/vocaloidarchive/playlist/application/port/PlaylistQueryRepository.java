package com.vocaloidarchive.playlist.application.port;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistDetailResult;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import java.util.List;
import java.util.Optional;

public interface PlaylistQueryRepository {
  List<PlaylistResult> findMyPlaylists(Long userId);
  Optional<PlaylistDetailResult> findDetailById(Long playlistId);
  PlaylistResult findResultAfterCreate(Long playlistId);
}
