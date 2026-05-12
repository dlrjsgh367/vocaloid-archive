package com.vocaloidarchive.playlist.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistJpaRepository extends JpaRepository<PlaylistEntity, Long> {

  @Query("SELECT p FROM PlaylistEntity p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
  List<PlaylistEntity> findAllByUserId(@Param("userId") Long userId);
}
