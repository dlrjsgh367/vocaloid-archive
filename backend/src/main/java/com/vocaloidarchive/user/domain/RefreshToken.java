package com.vocaloidarchive.user.domain;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class RefreshToken {

  private final Long id;
  private final User user;
  private final String tokenHash;
  private final LocalDateTime expiresAt;
  private final LocalDateTime createdAt;

  private RefreshToken(Long id, User user, String tokenHash,
                       LocalDateTime expiresAt, LocalDateTime createdAt) {
    this.id = id;
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public static RefreshToken issue(User user, String tokenHash, LocalDateTime expiresAt) {
    return new RefreshToken(null, user, tokenHash, expiresAt, null);
  }

  public static RefreshToken reconstitute(Long id, User user, String tokenHash,
                                          LocalDateTime expiresAt, LocalDateTime createdAt) {
    return new RefreshToken(id, user, tokenHash, expiresAt, createdAt);
  }

  public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }

  public Long getId() { return id; }

  public User getUser() { return user; }

  /** Delegating accessor so callers that only need the owner FK keep working. */
  public Long getUserId() { return user == null ? null : user.getId(); }

  public String getTokenHash() { return tokenHash; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
