package com.vocaloidarchive.like.application.port;

import com.vocaloidarchive.like.domain.Like;

public interface LikeRepository {
  boolean existsBy(Long userId, Long songId);
  Like save(Like like);
  void deleteBy(Long userId, Long songId);
}
