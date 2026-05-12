package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePlaylistUseCase {
  private final PlaylistRepository playlistRepo;
  private final SecurityUtil securityUtil;

  @Transactional
  public void invoke(Long id) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepo.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    playlistRepo.deleteById(id);
  }
}
