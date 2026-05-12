package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistDetailResult;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPlaylistDetailUseCase {
  private final PlaylistRepository playlistRepo;
  private final PlaylistQueryRepository queryRepo;
  private final SecurityUtil securityUtil;

  @Transactional(readOnly = true)
  public PlaylistDetailResult invoke(Long id) {
    Playlist playlist = playlistRepo.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isPublic()) {
      Long currentUserId;
      try {
        currentUserId = securityUtil.getCurrentUserId();
      } catch (BusinessException e) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      if (!playlist.isOwnedBy(currentUserId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
    }
    return queryRepo.findDetailById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
  }
}
