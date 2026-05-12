package com.vocaloidarchive.comment.interfaces.dto.response;

import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import java.time.LocalDateTime;

public record CommentResponse(Long id, String content, String username, LocalDateTime createdAt) {
  public static CommentResponse from(CommentResult r) {
    return new CommentResponse(r.id(), r.content(), r.username(), r.createdAt());
  }
}
