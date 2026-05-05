package com.vocaloidarchive.song.dto.response;

import com.vocaloidarchive.user.domain.User;

public record UserSummary(Long id, String username) {
  public static UserSummary from(User u) {
    return new UserSummary(u.getId(), u.getUsername());
  }
}
