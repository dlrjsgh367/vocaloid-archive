package com.vocaloidarchive.playlist.application.dto.command;

public record CreatePlaylistCommand(Long userId, String title, boolean isPublic) {}
