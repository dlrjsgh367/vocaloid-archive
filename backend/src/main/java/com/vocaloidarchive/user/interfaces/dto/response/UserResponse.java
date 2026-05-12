package com.vocaloidarchive.user.interfaces.dto.response;

import com.vocaloidarchive.user.application.dto.result.UserResult;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
  public static UserResponse from(UserResult r) {
    return new UserResponse(r.id(), r.username(), r.email(), r.createdAt());
  }
}
