package com.vocaloidarchive.comment.repository;

import com.vocaloidarchive.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  @Query(
      value = "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.song.id = :songId",
      countQuery = "SELECT count(c) FROM Comment c WHERE c.song.id = :songId"
  )
  Page<Comment> findWithUserBySongId(@Param("songId") Long songId, Pageable pageable);

  boolean existsByIdAndUserId(Long id, Long userId);
}
