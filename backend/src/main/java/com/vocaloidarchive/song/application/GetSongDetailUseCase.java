package com.vocaloidarchive.song.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.song.application.dto.result.SongDetailResult;
import com.vocaloidarchive.song.application.port.SongQueryRepository;
import com.vocaloidarchive.song.application.port.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetSongDetailUseCase {

  private final SongRepository songRepository;
  private final SongQueryRepository songQueryRepository;

  @Transactional
  public SongDetailResult invoke(Long id) {
    int affected = songRepository.incrementPlayCount(id);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    return songQueryRepository.findDetailById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.SONG_NOT_FOUND));
  }
}
