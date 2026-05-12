package com.vocaloidarchive.user.domain;

import java.time.LocalDateTime;

public class RefreshToken {

  private final Long id;
  private final Long userId;
  private final String tokenHash;
  private final LocalDateTime expiresAt;
  private final LocalDateTime createdAt;

  private RefreshToken(Long id, Long userId, String tokenHash,
                       LocalDateTime expiresAt, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime expiresAt) {
    return new RefreshToken(null, userId, tokenHash, expiresAt, null);
  }

  public static RefreshToken reconstitute(Long id, Long userId, String tokenHash,
                                          LocalDateTime expiresAt, LocalDateTime createdAt) {
    return new RefreshToken(id, userId, tokenHash, expiresAt, createdAt);
  }

  public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public String getTokenHash() { return tokenHash; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
