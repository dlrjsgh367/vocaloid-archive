package com.vocaloidarchive.user.domain;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class User {

  private final Long id;
  private final String username;
  private final String email;
  private final String passwordHash;
  private final String profileImageUrl;
  private final LocalDateTime createdAt;

  private User(Long id, String username, String email, String passwordHash,
               String profileImageUrl, LocalDateTime createdAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.profileImageUrl = profileImageUrl;
    this.createdAt = createdAt;
  }

  public static User newSignup(String username, String email, String passwordHash) {
    return new User(null, username, email, passwordHash, null, null);
  }

  public static User reconstitute(Long id, String username, String email, String passwordHash,
                                   String profileImageUrl, LocalDateTime createdAt) {
    return new User(id, username, email, passwordHash, profileImageUrl, createdAt);
  }

  /**
   * Id-only stub used on write paths where only the foreign key is known (e.g. the current user's
   * id from the security context). Other domains embed this so their {@code getXxxId()} accessors
   * resolve without loading a full User. See the migration plan's "reference() 팩토리" decision.
   */
  public static User reference(Long id) {
    return new User(id, null, null, null, null, null);
  }

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getProfileImageUrl() { return profileImageUrl; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
