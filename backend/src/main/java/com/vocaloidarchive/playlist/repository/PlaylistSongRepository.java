package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.playlist.domain.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {

  @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song WHERE ps.playlist.id = :playlistId ORDER BY ps.orderIndex ASC")
  List<PlaylistSong> findWithSongByPlaylistId(@Param("playlistId") Long playlistId);

  @Query("SELECT COALESCE(MAX(ps.orderIndex), 0) FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId")
  int findMaxOrderIndexByPlaylistId(@Param("playlistId") Long playlistId);

  boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

  @Modifying
  @Query("DELETE FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId AND ps.song.id = :songId")
  void deleteByPlaylistIdAndSongId(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

  long countByPlaylistId(Long playlistId);
}
