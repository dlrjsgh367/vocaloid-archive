package com.vocaloidarchive.like.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.repository.LikeRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

  private final LikeRepository likeRepository;
  private final SongRepository songRepository;
  private final UserJpaRepository userRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public LikeToggleResponse toggle(Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    LikeId likeId = new LikeId(userId, songId);
    boolean currentlyLiked = likeRepository.existsById(likeId);
    if (currentlyLiked) {
      likeRepository.deleteById(likeId);
    } else {
      UserEntity user = userRepository.getReferenceById(userId);
      Song song = songRepository.getReferenceById(songId);
      likeRepository.save(Like.of(user, song));
    }
    long likeCount = likeRepository.countBySongId(songId);
    return new LikeToggleResponse(!currentlyLiked, likeCount);
  }
}
