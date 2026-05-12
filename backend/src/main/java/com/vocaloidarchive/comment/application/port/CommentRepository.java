package com.vocaloidarchive.comment.application.port;

import com.vocaloidarchive.comment.domain.Comment;
import java.util.Optional;

public interface CommentRepository {
  Comment save(Comment comment);
  Optional<Long> findUserIdById(Long commentId);
  boolean existsById(Long id);
  void deleteById(Long id);
}
