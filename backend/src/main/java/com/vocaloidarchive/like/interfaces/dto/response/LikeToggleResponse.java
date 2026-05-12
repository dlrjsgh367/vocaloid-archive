package com.vocaloidarchive.like.interfaces.dto.response;

import com.vocaloidarchive.like.application.dto.result.ToggleLikeResult;

public record LikeToggleResponse(boolean liked, long likeCount) {
  public static LikeToggleResponse from(ToggleLikeResult r) {
    return new LikeToggleResponse(r.liked(), r.likeCount());
  }
}
