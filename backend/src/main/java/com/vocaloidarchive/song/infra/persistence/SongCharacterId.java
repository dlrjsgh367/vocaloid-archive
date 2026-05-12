package com.vocaloidarchive.song.infra.persistence;

import java.io.Serializable;
import java.util.Objects;

public class SongCharacterId implements Serializable {

  private Long song;
  private Long character;

  public SongCharacterId() {}

  public SongCharacterId(Long song, Long character) {
    this.song = song;
    this.character = character;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SongCharacterId that)) return false;
    return Objects.equals(song, that.song) && Objects.equals(character, that.character);
  }

  @Override
  public int hashCode() {
    return Objects.hash(song, character);
  }
}
