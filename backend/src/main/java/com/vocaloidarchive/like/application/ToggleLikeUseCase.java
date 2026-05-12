package com.vocaloidarchive.like.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.application.dto.result.ToggleLikeResult;
import com.vocaloidarchive.like.application.port.LikeQueryRepository;
import com.vocaloidarchive.like.application.port.LikeRepository;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleLikeUseCase {

  private final LikeRepository likeRepository;
  private final LikeQueryRepository likeQueryRepository;
  private final SongRepository songRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public ToggleLikeResult invoke(Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    boolean currentlyLiked = likeRepository.existsBy(userId, songId);
    if (currentlyLiked) {
      likeRepository.deleteBy(userId, songId);
    } else {
      likeRepository.save(Like.newLike(userId, songId));
    }
    return new ToggleLikeResult(!currentlyLiked, likeQueryRepository.countBySongId(songId));
  }
}
