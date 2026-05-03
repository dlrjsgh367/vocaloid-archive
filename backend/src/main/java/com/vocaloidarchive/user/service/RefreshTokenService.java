package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public TokenResponse issueTokens(User user) {
    String rawRefreshToken = jwtTokenProvider.generateRawRefreshToken();
    String tokenHash = hash(rawRefreshToken);
    LocalDateTime expiresAt = jwtTokenProvider.refreshTokenExpiry();
    refreshTokenRepository.save(RefreshToken.of(user, tokenHash, expiresAt));
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
    return new TokenResponse(accessToken, rawRefreshToken);
  }

  @Transactional
  public TokenResponse rotate(String rawRefreshToken) {
    String tokenHash = hash(rawRefreshToken);
    RefreshToken existing = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter(tokenHash, LocalDateTime.now())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    User user = existing.getUser();
    refreshTokenRepository.delete(existing);
    return issueTokens(user);
  }

  @Transactional
  public void revoke(String rawRefreshToken) {
    refreshTokenRepository.deleteByTokenHash(hash(rawRefreshToken));
  }

  private String hash(String token) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
