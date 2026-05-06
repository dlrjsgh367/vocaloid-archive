package com.vocaloidarchive.playlist.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlaylistSongId implements Serializable {

  private Long playlist;
  private Long song;

  public PlaylistSongId() {}

  public PlaylistSongId(Long playlist, Long song) {
    this.playlist = playlist;
    this.song = song;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlaylistSongId that)) return false;
    return Objects.equals(playlist, that.playlist) && Objects.equals(song, that.song);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playlist, song);
  }
}
