package com.vocaloidarchive.user.application.dto.result;

import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;

public record UserResult(Long id, String username, String email, LocalDateTime createdAt) {
  public static UserResult from(User u) {
    return new UserResult(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt());
  }
}
