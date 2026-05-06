package com.vocaloidarchive.playlist.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaylistSongAddRequest(@NotNull Long songId) {}
