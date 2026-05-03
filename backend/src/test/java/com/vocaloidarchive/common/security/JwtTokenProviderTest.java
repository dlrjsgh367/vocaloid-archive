package com.vocaloidarchive.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

  // 256-bit key encoded in Base64 (required by HS256)
  private static final String SECRET = Base64.getEncoder()
      .encodeToString("test-secret-key-256bit-long-enough-x".getBytes());

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(SECRET, 30, 14);
  }

  @Test
  void givenUserId_whenGenerateAndParse_thenReturnsSameUserId() {
    String token = provider.generateAccessToken(42L);
    assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
  }

  @Test
  void givenExpiredToken_whenGetUserId_thenThrowsExpiredJwtException() {
    JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, -1, 14);
    String expiredToken = shortLived.generateAccessToken(1L);
    assertThatThrownBy(() -> provider.getUserIdFromToken(expiredToken))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void givenInvalidToken_whenGetUserId_thenThrowsJwtException() {
    assertThatThrownBy(() -> provider.getUserIdFromToken("not.a.valid.jwt"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void generateRawRefreshToken_returnsDifferentValuesEachCall() {
    String t1 = provider.generateRawRefreshToken();
    String t2 = provider.generateRawRefreshToken();
    assertThat(t1).isNotEqualTo(t2);
  }

  @Test
  void refreshTokenExpiry_isFourteenDaysFromNow() {
    var expiry = provider.refreshTokenExpiry();
    var now = java.time.LocalDateTime.now();
    assertThat(expiry).isAfter(now.plusDays(13));
    assertThat(expiry).isBefore(now.plusDays(15));
  }
}
