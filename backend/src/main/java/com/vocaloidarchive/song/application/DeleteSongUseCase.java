package com.vocaloidarchive.song.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.application.port.SongRepository;
import com.vocaloidarchive.song.domain.Song;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteSongUseCase {

  private final SongRepository songRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public void invoke(Long id) {
    Long currentUserId = securityUtil.getCurrentUserId();
    Song song = songRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.SONG_NOT_FOUND));
    if (!song.isRegisteredBy(currentUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    songRepository.deleteById(id);
  }
}
