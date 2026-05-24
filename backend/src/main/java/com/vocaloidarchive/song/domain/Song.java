package com.vocaloidarchive.song.domain;

import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public class Song {
  private final Long id;
  private final User registeredBy;
  private final String title;
  private final String youtubeUrl;
  private final String niconicoUrl;
  private final String thumbnailUrl;
  private final Integer bpm;
  private final Mood mood;
  private final Integer playCount;
  private final LocalDateTime createdAt;

  private Song(Long id, User registeredBy, String title, String youtubeUrl, String niconicoUrl,
      String thumbnailUrl, Integer bpm, Mood mood, Integer playCount, LocalDateTime createdAt) {
    this.id = id;
    this.registeredBy = registeredBy;
    this.title = title;
    this.youtubeUrl = youtubeUrl;
    this.niconicoUrl = niconicoUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.bpm = bpm;
    this.mood = mood;
    this.playCount = playCount;
    this.createdAt = createdAt;
  }

  public static Song newSong(User registeredBy, String title, String youtubeUrl,
      String niconicoUrl, String thumbnailUrl, Integer bpm, Mood mood) {
    return new Song(null, registeredBy, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, 0, null);
  }

  public static Song reconstitute(Long id, User registeredBy, String title, String youtubeUrl,
      String niconicoUrl, String thumbnailUrl, Integer bpm, Mood mood, Integer playCount,
      LocalDateTime createdAt) {
    return new Song(id, registeredBy, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, playCount, createdAt);
  }

  public boolean isRegisteredBy(Long userId) {
    return registeredBy != null && registeredBy.getId() != null
        && registeredBy.getId().equals(userId);
  }

  public Long getId() { return id; }

  public User getRegisteredBy() { return registeredBy; }

  /** Delegating accessor so write paths and callers that only need the FK keep working. */
  public Long getRegisteredById() { return registeredBy == null ? null : registeredBy.getId(); }

  public String getTitle() { return title; }
  public String getYoutubeUrl() { return youtubeUrl; }
  public String getNiconicoUrl() { return niconicoUrl; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public Integer getBpm() { return bpm; }
  public Mood getMood() { return mood; }
  public Integer getPlayCount() { return playCount; }
  public LocalDateTime getCreatedAt() { return createdAt; }

  /** Id-only stub for embedding as a foreign-key reference (see {@link User#reference}). */
  public static Song reference(Long id) {
    return new Song(id, null, null, null, null, null, null, null, null, null);
  }
}
