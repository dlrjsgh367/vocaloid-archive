package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.song.infra.persistence.SongEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_songs")
@IdClass(PlaylistSongId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSongEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private PlaylistEntity playlist;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private SongEntity song;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  public static PlaylistSongEntity of(PlaylistEntity playlist, SongEntity song, int orderIndex) {
    PlaylistSongEntity ps = new PlaylistSongEntity();
    ps.playlist = playlist;
    ps.song = song;
    ps.orderIndex = orderIndex;
    return ps;
  }
}
