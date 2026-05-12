package com.vocaloidarchive.comment.dto.response;

import com.vocaloidarchive.comment.infra.persistence.CommentEntity;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String content,
    String username,
    LocalDateTime createdAt
) {
  public static CommentResponse from(CommentEntity c) {
    return new CommentResponse(
        c.getId(),
        c.getContent(),
        c.getUser().getUsername(),
        c.getCreatedAt()
    );
  }
}
