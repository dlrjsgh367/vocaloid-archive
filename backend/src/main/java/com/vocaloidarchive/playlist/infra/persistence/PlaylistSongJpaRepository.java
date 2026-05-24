package com.vocaloidarchive.playlist.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PlaylistSongJpaRepository extends JpaRepository<PlaylistSongEntity, PlaylistSongId> {

  /** Interface projection for the most-frequent character across a playlist's songs. */
  interface TopCharacterView {
    String getName();
    String getColorHex();
  }

  @Query(value = """
      SELECT c.name AS name, c.color_hex AS colorHex
      FROM playlist_songs ps
      JOIN song_characters sc ON sc.song_id = ps.song_id
      JOIN characters c ON c.id = sc.character_id
      WHERE ps.playlist_id = :playlistId
      GROUP BY c.id, c.name, c.color_hex
      ORDER BY COUNT(*) DESC, c.id ASC
      LIMIT 1
      """, nativeQuery = true)
  Optional<TopCharacterView> findTopCharacterByPlaylistId(@Param("playlistId") Long playlistId);

  @Query(value = """
      SELECT COUNT(*)
      FROM playlist_songs ps
      JOIN likes l ON l.song_id = ps.song_id
      WHERE ps.playlist_id = :playlistId
      """, nativeQuery = true)
  long sumLikesByPlaylistId(@Param("playlistId") Long playlistId);

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
