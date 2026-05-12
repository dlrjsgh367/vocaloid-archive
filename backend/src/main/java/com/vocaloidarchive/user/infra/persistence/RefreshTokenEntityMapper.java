package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.RefreshToken;

final class RefreshTokenEntityMapper {

  private RefreshTokenEntityMapper() {}

  static RefreshToken toDomain(RefreshTokenEntity e) {
    if (e == null) return null;
    return RefreshToken.reconstitute(
        e.getId(), e.getUser().getId(), e.getTokenHash(),
        e.getExpiresAt(), e.getCreatedAt());
  }

  static RefreshTokenEntity toNewEntity(RefreshToken t, UserEntity userRef) {
    return RefreshTokenEntity.of(userRef, t.getTokenHash(), t.getExpiresAt());
  }
}
