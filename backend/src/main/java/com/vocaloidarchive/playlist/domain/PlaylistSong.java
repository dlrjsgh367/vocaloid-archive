package com.vocaloidarchive.playlist.domain;

import com.vocaloidarchive.song.domain.Song;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_songs")
@IdClass(PlaylistSongId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSong {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  public static PlaylistSong of(Playlist playlist, Song song, int orderIndex) {
    PlaylistSong ps = new PlaylistSong();
    ps.playlist = playlist;
    ps.song = song;
    ps.orderIndex = orderIndex;
    return ps;
  }
}
