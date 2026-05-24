package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistDetailResult;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import com.vocaloidarchive.common.util.YoutubeUtil;
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
              YoutubeUtil.resolveThumbnailUrl(
                  ps.getSong().getThumbnailUrl(), ps.getSong().getYoutubeUrl()),
              ps.getOrderIndex()))
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

  @Override
  public Optional<PlaylistCardData> findCardDataByShareCode(String shareCode) {
    return jpa.findByShareCode(shareCode).map(p -> {
      Long pid = p.getId();
      List<PlaylistCardData.SongLine> songs = songJpa.findWithSongByPlaylistId(pid).stream()
          .map(ps -> new PlaylistCardData.SongLine(
              ps.getSong().getId(),
              ps.getSong().getTitle(),
              YoutubeUtil.resolveThumbnailUrl(
                  ps.getSong().getThumbnailUrl(), ps.getSong().getYoutubeUrl()),
              ps.getSong().getMood() == null ? null : ps.getSong().getMood().name()))
          .toList();

      String themeColorHex = PlaylistCardData.DEFAULT_THEME_HEX;
      String primaryCharName = null;
      var top = songJpa.findTopCharacterByPlaylistId(pid);
      if (top.isPresent()) {
        primaryCharName = top.get().getName();
        if (top.get().getColorHex() != null && !top.get().getColorHex().isBlank()) {
          themeColorHex = top.get().getColorHex();
        }
      }

      long likeSum = songJpa.sumLikesByPlaylistId(pid);

      return new PlaylistCardData(pid, p.getShareCode(), p.getTitle(), p.getUser().getUsername(),
          songs.size(), likeSum, themeColorHex, primaryCharName, p.isPublic(), songs);
    });
  }
}
