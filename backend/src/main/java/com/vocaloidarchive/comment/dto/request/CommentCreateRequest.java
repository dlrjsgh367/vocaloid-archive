package com.vocaloidarchive.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
    @NotBlank @Size(min = 1, max = 500) String content
) {}
