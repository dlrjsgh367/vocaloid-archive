package com.vocaloidarchive.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessTokenExpiryMinutes;
  private final long refreshTokenExpiryDays;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-expiry-minutes}") long accessTokenExpiryMinutes,
      @Value("${app.jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    this.refreshTokenExpiryDays = refreshTokenExpiryDays;
  }

  public String generateAccessToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpiryMinutes * 60 * 1000L);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public String generateRawRefreshToken() {
    return UUID.randomUUID().toString();
  }

  public LocalDateTime refreshTokenExpiry() {
    return LocalDateTime.now().plusDays(refreshTokenExpiryDays);
  }

  // Throws ExpiredJwtException if expired, JwtException if otherwise invalid.
  // Callers (JwtAuthenticationFilter) catch these to set the appropriate error code.
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return Long.parseLong(claims.getSubject());
  }
}
