package com.vocaloidarchive.song.dto.request;

public enum SongSort {
  LATEST,
  POPULAR,
  PLAYED;

  public static SongSort orDefault(SongSort value) {
    return value == null ? LATEST : value;
  }
}
