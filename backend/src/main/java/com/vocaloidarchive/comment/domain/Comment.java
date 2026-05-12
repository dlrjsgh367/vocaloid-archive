package com.vocaloidarchive.comment.domain;

import java.time.LocalDateTime;

public class Comment {
  private final Long id;
  private final Long userId;
  private final Long songId;
  private final String content;
  private final LocalDateTime createdAt;

  private Comment(Long id, Long userId, Long songId, String content, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.songId = songId;
    this.content = content;
    this.createdAt = createdAt;
  }

  public static Comment newComment(Long userId, Long songId, String content) {
    return new Comment(null, userId, songId, content, null);
  }

  public static Comment reconstitute(
      Long id, Long userId, Long songId, String content, LocalDateTime createdAt) {
    return new Comment(id, userId, songId, content, createdAt);
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getSongId() {
    return songId;
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
