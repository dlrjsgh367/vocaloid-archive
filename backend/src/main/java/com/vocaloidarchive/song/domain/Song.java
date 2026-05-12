package com.vocaloidarchive.song.domain;

import java.time.LocalDateTime;

public class Song {
  private final Long id;
  private final Long registeredById;
  private final String title;
  private final String youtubeUrl;
  private final String niconicoUrl;
  private final String thumbnailUrl;
  private final Integer bpm;
  private final Mood mood;
  private final Integer playCount;
  private final LocalDateTime createdAt;

  private Song(Long id, Long registeredById, String title, String youtubeUrl, String niconicoUrl,
      String thumbnailUrl, Integer bpm, Mood mood, Integer playCount, LocalDateTime createdAt) {
    this.id = id;
    this.registeredById = registeredById;
    this.title = title;
    this.youtubeUrl = youtubeUrl;
    this.niconicoUrl = niconicoUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.bpm = bpm;
    this.mood = mood;
    this.playCount = playCount;
    this.createdAt = createdAt;
  }

  public static Song newSong(Long registeredById, String title, String youtubeUrl,
      String niconicoUrl, String thumbnailUrl, Integer bpm, Mood mood) {
    return new Song(null, registeredById, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, 0, null);
  }

  public static Song reconstitute(Long id, Long registeredById, String title, String youtubeUrl,
      String niconicoUrl, String thumbnailUrl, Integer bpm, Mood mood, Integer playCount,
      LocalDateTime createdAt) {
    return new Song(id, registeredById, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, playCount, createdAt);
  }

  public boolean isRegisteredBy(Long userId) {
    return registeredById != null && registeredById.equals(userId);
  }

  public Long getId() { return id; }
  public Long getRegisteredById() { return registeredById; }
  public String getTitle() { return title; }
  public String getYoutubeUrl() { return youtubeUrl; }
  public String getNiconicoUrl() { return niconicoUrl; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public Integer getBpm() { return bpm; }
  public Mood getMood() { return mood; }
  public Integer getPlayCount() { return playCount; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
