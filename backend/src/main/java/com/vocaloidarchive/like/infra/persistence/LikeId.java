package com.vocaloidarchive.like.infra.persistence;

import java.io.Serializable;
import java.util.Objects;

public class LikeId implements Serializable {

  private Long user;
  private Long song;

  public LikeId() {}

  public LikeId(Long user, Long song) {
    this.user = user;
    this.song = song;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LikeId that)) return false;
    return Objects.equals(user, that.user) && Objects.equals(song, that.song);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, song);
  }
}
