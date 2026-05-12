package com.vocaloidarchive.playlist.application.dto.result;

import java.time.LocalDateTime;

public record PlaylistResult(Long id, String title, boolean isPublic,
    String ownerUsername, long songCount, LocalDateTime createdAt) {}
