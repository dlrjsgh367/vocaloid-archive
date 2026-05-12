package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistDetailResult;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {
  private final PlaylistJpaRepository jpa;
  private final PlaylistSongJpaRepository songJpa;

  @Override
  public List<PlaylistResult> findMyPlaylists(Long userId) {
    return jpa.findAllByUserId(userId).stream()
        .map(p -> new PlaylistResult(p.getId(), p.getTitle(), p.isPublic(),
            p.getUser().getUsername(), songJpa.countByPlaylistId(p.getId()), p.getCreatedAt()))
        .toList();
  }

  @Override
  public Optional<PlaylistDetailResult> findDetailById(Long playlistId) {
    return jpa.findById(playlistId).map(p -> {
      List<PlaylistDetailResult.SongItem> items = songJpa.findWithSongByPlaylistId(playlistId).stream()
          .map(ps -> new PlaylistDetailResult.SongItem(
              ps.getSong().getId(), ps.getSong().getTitle(),
              ps.getSong().getThumbnailUrl(), ps.getOrderIndex()))
          .toList();
      return new PlaylistDetailResult(p.getId(), p.getTitle(), p.isPublic(),
          p.getUser().getUsername(), items, p.getCreatedAt());
    });
  }

  @Override
  public PlaylistResult findResultAfterCreate(Long playlistId) {
    PlaylistEntity p = jpa.findById(playlistId)
        .orElseThrow(() -> new IllegalStateException("just-created playlist not found: " + playlistId));
    return new PlaylistResult(p.getId(), p.getTitle(), p.isPublic(),
        p.getUser().getUsername(), 0L, p.getCreatedAt());
  }
}
