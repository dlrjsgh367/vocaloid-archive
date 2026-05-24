package com.vocaloidarchive.playlist.domain;

import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class Playlist {
  private final Long id;
  private final User user;
  private final String title;
  private final boolean isPublic;
  private final String shareCode;
  private final LocalDateTime createdAt;

  private Playlist(Long id, User user, String title, boolean isPublic, String shareCode,
      LocalDateTime createdAt) {
    this.id = id;
    this.user = user;
    this.title = title;
    this.isPublic = isPublic;
    this.shareCode = shareCode;
    this.createdAt = createdAt;
  }

  public static Playlist newPlaylist(User user, String title, boolean isPublic) {
    return new Playlist(null, user, title, isPublic, null, null);
  }

  public static Playlist reconstitute(Long id, User user, String title, boolean isPublic,
      String shareCode, LocalDateTime createdAt) {
    return new Playlist(id, user, title, isPublic, shareCode, createdAt);
  }

  public boolean isOwnedBy(Long candidateUserId) {
    return user != null && user.getId() != null && user.getId().equals(candidateUserId);
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  /** Delegating accessor so write paths and ownership checks keep working off the FK. */
  public Long getUserId() {
    return user == null ? null : user.getId();
  }

  public String getTitle() {
    return title;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public String getShareCode() {
    return shareCode;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
