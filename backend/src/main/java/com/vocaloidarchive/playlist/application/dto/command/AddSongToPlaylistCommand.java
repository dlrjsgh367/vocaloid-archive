package com.vocaloidarchive.playlist.application.dto.command;

public record AddSongToPlaylistCommand(Long playlistId, Long songId, Long userId) {}
