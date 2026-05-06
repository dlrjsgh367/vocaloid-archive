package com.vocaloidarchive.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank @Size(max = 200) String title,
    boolean isPublic
) {}
