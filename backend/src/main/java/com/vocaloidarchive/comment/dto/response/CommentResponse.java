package com.vocaloidarchive.comment.dto.response;

import com.vocaloidarchive.comment.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String content,
    String username,
    LocalDateTime createdAt
) {
  public static CommentResponse from(Comment c) {
    return new CommentResponse(
        c.getId(),
        c.getContent(),
        c.getUser().getUsername(),
        c.getCreatedAt()
    );
  }
}
