package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.playlist.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

  @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
  List<Playlist> findAllByUserId(@Param("userId") Long userId);
}
