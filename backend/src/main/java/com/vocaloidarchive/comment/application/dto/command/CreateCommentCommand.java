package com.vocaloidarchive.comment.application.dto.command;

public record CreateCommentCommand(Long songId, Long userId, String content) {}
