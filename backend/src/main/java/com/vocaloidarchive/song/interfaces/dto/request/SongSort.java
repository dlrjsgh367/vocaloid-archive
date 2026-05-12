package com.vocaloidarchive.song.interfaces.dto.request;

import com.vocaloidarchive.song.application.SongSortKey;

public enum SongSort {
  LATEST,
  POPULAR,
  PLAYED;

  public static SongSort orDefault(SongSort value) {
    return value == null ? LATEST : value;
  }

  public SongSortKey toSortKey() {
    return switch (this) {
      case LATEST  -> SongSortKey.LATEST;
      case POPULAR -> SongSortKey.POPULAR;
      case PLAYED  -> SongSortKey.PLAYED;
    };
  }
}
