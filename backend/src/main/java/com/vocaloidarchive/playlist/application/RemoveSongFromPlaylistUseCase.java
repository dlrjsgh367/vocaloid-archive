package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.application.port.PlaylistSongRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveSongFromPlaylistUseCase {
  private final PlaylistRepository playlistRepo;
  private final PlaylistSongRepository songRepo;
  private final SecurityUtil securityUtil;

  @Transactional
  public void invoke(Long playlistId, Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepo.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    songRepo.deleteBy(playlistId, songId);
  }
}
