package com.vocaloidarchive.playlist.domain;

import java.time.LocalDateTime;

public class Playlist {
  private final Long id;
  private final Long userId;
  private final String title;
  private final boolean isPublic;
  private final LocalDateTime createdAt;

  private Playlist(Long id, Long userId, String title, boolean isPublic, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.title = title;
    this.isPublic = isPublic;
    this.createdAt = createdAt;
  }

  public static Playlist newPlaylist(Long userId, String title, boolean isPublic) {
    return new Playlist(null, userId, title, isPublic, null);
  }

  public static Playlist reconstitute(Long id, Long userId, String title, boolean isPublic, LocalDateTime createdAt) {
    return new Playlist(id, userId, title, isPublic, createdAt);
  }

  public boolean isOwnedBy(Long candidateUserId) {
    return userId != null && userId.equals(candidateUserId);
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTitle() {
    return title;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
