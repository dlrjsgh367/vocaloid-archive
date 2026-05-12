package com.vocaloidarchive.like.application.port;

public interface LikeQueryRepository {
  long countBySongId(Long songId);
}
