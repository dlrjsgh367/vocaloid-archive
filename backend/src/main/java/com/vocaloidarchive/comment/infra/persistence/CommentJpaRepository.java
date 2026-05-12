package com.vocaloidarchive.comment.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentJpaRepository extends JpaRepository<CommentEntity, Long> {

  @Query(
      value = "SELECT c FROM CommentEntity c JOIN FETCH c.user WHERE c.song.id = :songId",
      countQuery = "SELECT count(c) FROM CommentEntity c WHERE c.song.id = :songId"
  )
  Page<CommentEntity> findWithUserBySongId(@Param("songId") Long songId, Pageable pageable);

  boolean existsByIdAndUserId(Long id, Long userId);
}
