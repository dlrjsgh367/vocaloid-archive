package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.common.util.ShareCodeGenerator;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lazily issues (or returns the existing) share code for a public playlist. Only the owner may
 * share, and only public playlists are shareable. The code is generated once and then fixed.
 */
@Service
@RequiredArgsConstructor
public class EnsureShareCodeUseCase {

  private static final int MAX_ATTEMPTS = 5;

  private final PlaylistRepository playlistRepo;
  private final SecurityUtil securityUtil;

  @Transactional
  public String invoke(Long playlistId) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepo.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    if (!playlist.isOwnedBy(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!playlist.isPublic()) {
      throw new BusinessException(ErrorCode.PLAYLIST_NOT_PUBLIC);
    }
    if (playlist.getShareCode() != null && !playlist.getShareCode().isBlank()) {
      return playlist.getShareCode();
    }

    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String code = ShareCodeGenerator.generate();
      if (!playlistRepo.existsByShareCode(code)) {
        playlistRepo.updateShareCode(playlistId, code);
        return code;
      }
    }
    throw new BusinessException(ErrorCode.SHARE_CODE_GENERATION_FAILED);
  }
}
