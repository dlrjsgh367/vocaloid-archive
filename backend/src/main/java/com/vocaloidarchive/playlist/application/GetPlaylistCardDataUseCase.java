package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves public card data by share code. Private (or unknown) codes are reported as not found so
 * existence/visibility is not leaked.
 */
@Service
@RequiredArgsConstructor
public class GetPlaylistCardDataUseCase {

  private final PlaylistQueryRepository queryRepo;

  @Transactional(readOnly = true)
  public PlaylistCardData invoke(String shareCode) {
    return queryRepo.findCardDataByShareCode(shareCode)
        .filter(PlaylistCardData::isPublic)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
  }
}
