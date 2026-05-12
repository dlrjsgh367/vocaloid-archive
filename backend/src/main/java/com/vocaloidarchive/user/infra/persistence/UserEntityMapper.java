package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.User;

final class UserEntityMapper {

  private UserEntityMapper() {}

  static User toDomain(UserEntity e) {
    if (e == null) return null;
    return User.reconstitute(
        e.getId(), e.getUsername(), e.getEmail(), e.getPasswordHash(),
        e.getProfileImageUrl(), e.getCreatedAt());
  }

  static UserEntity toNewEntity(User u) {
    return UserEntity.of(u.getUsername(), u.getEmail(), u.getPasswordHash());
  }
}
