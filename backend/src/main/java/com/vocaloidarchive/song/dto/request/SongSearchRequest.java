package com.vocaloidarchive.song.dto.request;

import com.vocaloidarchive.song.domain.Mood;

public record SongSearchRequest(
    String keyword,
    Mood mood,
    Long characterId,
    Long tagId,
    SongSort sort
) {
  public SongSort sortOrDefault() {
    return SongSort.orDefault(sort);
  }
}
