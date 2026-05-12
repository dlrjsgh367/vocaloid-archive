package com.vocaloidarchive.song.dto.response;

import com.vocaloidarchive.user.infra.persistence.UserEntity;

public record UserSummary(Long id, String username) {
  public static UserSummary from(UserEntity u) {
    return new UserSummary(u.getId(), u.getUsername());
  }
}
