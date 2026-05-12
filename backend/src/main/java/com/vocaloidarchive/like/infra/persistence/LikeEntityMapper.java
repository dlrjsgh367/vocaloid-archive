package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.like.domain.Like;

final class LikeEntityMapper {

  private LikeEntityMapper() {}

  static Like toDomain(LikeEntity e) {
    return e == null ? null : Like.reconstitute(
        e.getUser().getId(), e.getSong().getId(), e.getLikedAt());
  }
}
