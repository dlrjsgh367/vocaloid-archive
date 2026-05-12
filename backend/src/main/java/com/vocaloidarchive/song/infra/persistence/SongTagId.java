package com.vocaloidarchive.song.infra.persistence;

import java.io.Serializable;
import java.util.Objects;

public class SongTagId implements Serializable {

  private Long song;
  private Long tag;

  public SongTagId() {}

  public SongTagId(Long song, Long tag) {
    this.song = song;
    this.tag = tag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SongTagId that)) return false;
    return Objects.equals(song, that.song) && Objects.equals(tag, that.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(song, tag);
  }
}
