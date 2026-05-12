package com.vocaloidarchive.comment.infra.persistence;

import com.vocaloidarchive.comment.domain.Comment;

final class CommentEntityMapper {
  private CommentEntityMapper() {}

  static Comment toDomain(CommentEntity e) {
    return e == null
        ? null
        : Comment.reconstitute(
            e.getId(), e.getUser().getId(), e.getSong().getId(), e.getContent(), e.getCreatedAt());
  }
}
