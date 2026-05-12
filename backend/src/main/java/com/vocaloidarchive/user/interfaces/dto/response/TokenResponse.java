package com.vocaloidarchive.user.interfaces.dto.response;

import com.vocaloidarchive.user.application.dto.result.TokenResult;

public record TokenResponse(String accessToken, String refreshToken) {
  public static TokenResponse from(TokenResult r) {
    return new TokenResponse(r.accessToken(), r.refreshToken());
  }
}
