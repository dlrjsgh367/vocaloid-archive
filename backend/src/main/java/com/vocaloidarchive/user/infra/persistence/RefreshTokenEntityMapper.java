package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.RefreshToken;

/** Domain → new entity helper. Entity → domain now lives in {@link RefreshTokenDomainMapper}. */
final class RefreshTokenEntityMapper {

  private RefreshTokenEntityMapper() {}

  static RefreshTokenEntity toNewEntity(RefreshToken t, UserEntity userRef) {
    return RefreshTokenEntity.of(userRef, t.getTokenHash(), t.getExpiresAt());
  }
}
