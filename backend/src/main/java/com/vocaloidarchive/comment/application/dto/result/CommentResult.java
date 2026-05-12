package com.vocaloidarchive.comment.application.dto.result;

import java.time.LocalDateTime;

public record CommentResult(Long id, String content, String username, LocalDateTime createdAt) {}
