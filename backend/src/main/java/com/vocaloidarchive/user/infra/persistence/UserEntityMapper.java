package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.User;

/** Domain → new entity helper. Entity → domain now lives in {@link UserDomainMapper}. */
final class UserEntityMapper {

  private UserEntityMapper() {}

  static UserEntity toNewEntity(User u) {
    return UserEntity.of(u.getUsername(), u.getEmail(), u.getPasswordHash());
  }
}
