package com.vocaloidarchive.comment.domain;

import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class Comment {
  private final Long id;
  private final User user;
  private final Song song;
  private final String content;
  private final LocalDateTime createdAt;

  private Comment(Long id, User user, Song song, String content, LocalDateTime createdAt) {
    this.id = id;
    this.user = user;
    this.song = song;
    this.content = content;
    this.createdAt = createdAt;
  }

  public static Comment newComment(User user, Song song, String content) {
    return new Comment(null, user, song, content, null);
  }

  public static Comment reconstitute(
      Long id, User user, Song song, String content, LocalDateTime createdAt) {
    return new Comment(id, user, song, content, createdAt);
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Song getSong() {
    return song;
  }

  /** Delegating accessors so write paths keep working off the foreign keys. */
  public Long getUserId() {
    return user == null ? null : user.getId();
  }

  public Long getSongId() {
    return song == null ? null : song.getId();
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
