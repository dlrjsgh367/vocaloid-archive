package com.vocaloidarchive.comment.application;

import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import com.vocaloidarchive.comment.application.port.CommentQueryRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.song.infra.persistence.SongJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCommentsUseCase {
  private final CommentQueryRepository commentQueryRepository;
  private final SongJpaRepository songRepository;

  @Transactional(readOnly = true)
  public PageResponse<CommentResult> invoke(Long songId, Pageable pageable) {
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    return commentQueryRepository.listBySong(songId, pageable);
  }
}
