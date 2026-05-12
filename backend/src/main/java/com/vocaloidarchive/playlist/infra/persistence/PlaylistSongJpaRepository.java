package com.vocaloidarchive.playlist.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PlaylistSongJpaRepository extends JpaRepository<PlaylistSongEntity, PlaylistSongId> {

  @Query("SELECT ps FROM PlaylistSongEntity ps JOIN FETCH ps.song WHERE ps.playlist.id = :playlistId ORDER BY ps.orderIndex ASC")
  List<PlaylistSongEntity> findWithSongByPlaylistId(@Param("playlistId") Long playlistId);

  @Query("SELECT COALESCE(MAX(ps.orderIndex), 0) FROM PlaylistSongEntity ps WHERE ps.playlist.id = :playlistId")
  int findMaxOrderIndexByPlaylistId(@Param("playlistId") Long playlistId);

  boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

  @Transactional
  @Modifying
  @Query("DELETE FROM PlaylistSongEntity ps WHERE ps.playlist.id = :playlistId AND ps.song.id = :songId")
  void deleteByPlaylistIdAndSongId(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

  long countByPlaylistId(Long playlistId);
}
