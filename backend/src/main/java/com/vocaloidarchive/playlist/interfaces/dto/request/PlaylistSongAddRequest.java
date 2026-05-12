package com.vocaloidarchive.playlist.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaylistSongAddRequest(@NotNull Long songId) {}
