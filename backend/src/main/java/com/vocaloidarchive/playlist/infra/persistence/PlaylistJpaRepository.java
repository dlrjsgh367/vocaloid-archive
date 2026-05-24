package com.vocaloidarchive.playlist.infra.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistJpaRepository extends JpaRepository<PlaylistEntity, Long> {

  // Command-path load: fetch user so the mapper's @Condition guard passes and the reconstituted
  // Playlist's owner is present for isOwnedBy(...) ownership checks.
  @EntityGraph(attributePaths = {"user"})
  Optional<PlaylistEntity> findWithUserById(Long id);

  @Query("SELECT p FROM PlaylistEntity p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
  List<PlaylistEntity> findAllByUserId(@Param("userId") Long userId);

  @Query("SELECT p FROM PlaylistEntity p JOIN FETCH p.user WHERE p.shareCode = :shareCode")
  Optional<PlaylistEntity> findByShareCode(@Param("shareCode") String shareCode);

  boolean existsByShareCode(String shareCode);
}
