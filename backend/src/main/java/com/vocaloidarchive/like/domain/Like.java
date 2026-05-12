package com.vocaloidarchive.like.domain;

import java.time.LocalDateTime;

public class Like {

  private final Long userId;
  private final Long songId;
  private final LocalDateTime likedAt;

  private Like(Long userId, Long songId, LocalDateTime likedAt) {
    this.userId = userId;
    this.songId = songId;
    this.likedAt = likedAt;
  }

  public static Like newLike(Long userId, Long songId) {
    return new Like(userId, songId, null);
  }

  public static Like reconstitute(Long userId, Long songId, LocalDateTime likedAt) {
    return new Like(userId, songId, likedAt);
  }

  public Long getUserId() { return userId; }
  public Long getSongId() { return songId; }
  public LocalDateTime getLikedAt() { return likedAt; }
}
