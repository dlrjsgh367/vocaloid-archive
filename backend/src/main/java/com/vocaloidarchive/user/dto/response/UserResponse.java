package com.vocaloidarchive.user.dto.response;

import com.vocaloidarchive.user.infra.persistence.UserEntity;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String profileImageUrl,
    LocalDateTime createdAt
) {
  public static UserResponse from(UserEntity user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getCreatedAt());
  }
}
