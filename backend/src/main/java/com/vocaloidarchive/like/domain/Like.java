package com.vocaloidarchive.like.domain;

import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class Like {

  private final User user;
  private final Song song;
  private final LocalDateTime likedAt;

  private Like(User user, Song song, LocalDateTime likedAt) {
    this.user = user;
    this.song = song;
    this.likedAt = likedAt;
  }

  public static Like newLike(User user, Song song) {
    return new Like(user, song, null);
  }

  public static Like reconstitute(User user, Song song, LocalDateTime likedAt) {
    return new Like(user, song, likedAt);
  }

  public User getUser() { return user; }

  public Song getSong() { return song; }

  /** Delegating accessors so write paths keep working off the foreign keys. */
  public Long getUserId() { return user == null ? null : user.getId(); }

  public Long getSongId() { return song == null ? null : song.getId(); }

  public LocalDateTime getLikedAt() { return likedAt; }
}
