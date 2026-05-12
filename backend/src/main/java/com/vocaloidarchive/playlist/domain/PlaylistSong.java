package com.vocaloidarchive.playlist.domain;

public class PlaylistSong {
  private final Long playlistId;
  private final Long songId;
  private final int orderIndex;

  private PlaylistSong(Long playlistId, Long songId, int orderIndex) {
    this.playlistId = playlistId;
    this.songId = songId;
    this.orderIndex = orderIndex;
  }

  public static PlaylistSong of(Long playlistId, Long songId, int orderIndex) {
    return new PlaylistSong(playlistId, songId, orderIndex);
  }

  public Long getPlaylistId() {
    return playlistId;
  }

  public Long getSongId() {
    return songId;
  }

  public int getOrderIndex() {
    return orderIndex;
  }
}
