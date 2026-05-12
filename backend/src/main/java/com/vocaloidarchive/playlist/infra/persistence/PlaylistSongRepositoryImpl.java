package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.playlist.application.port.PlaylistSongRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaylistSongRepositoryImpl implements PlaylistSongRepository {
  private final PlaylistSongJpaRepository jpa;
  private final PlaylistJpaRepository playlistJpa;
  private final SongRepository songJpa;

  @Override
  public void add(Long playlistId, Long songId, int orderIndex) {
    PlaylistEntity playlistRef = playlistJpa.getReferenceById(playlistId);
    Song songRef = songJpa.getReferenceById(songId);
    jpa.save(PlaylistSongEntity.of(playlistRef, songRef, orderIndex));
  }

  @Override
  public boolean existsBy(Long playlistId, Long songId) {
    return jpa.existsByPlaylistIdAndSongId(playlistId, songId);
  }

  @Override
  public void deleteBy(Long playlistId, Long songId) {
    jpa.deleteByPlaylistIdAndSongId(playlistId, songId);
  }

  @Override
  public int findMaxOrderIndex(Long playlistId) {
    return jpa.findMaxOrderIndexByPlaylistId(playlistId);
  }
}
