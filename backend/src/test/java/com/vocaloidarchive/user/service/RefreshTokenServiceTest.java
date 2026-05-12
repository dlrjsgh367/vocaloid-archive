package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.infra.persistence.RefreshTokenEntity;
import com.vocaloidarchive.user.infra.persistence.RefreshTokenJpaRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock RefreshTokenJpaRepository refreshTokenRepository;
  @Mock JwtTokenProvider jwtTokenProvider;
  @InjectMocks RefreshTokenService refreshTokenService;

  private final UserEntity user = UserEntity.of("user", "user@test.com", "hash");

  @Test
  void givenUser_whenIssueTokens_thenSavesHashAndReturnsTokens() {
    // given
    given(jwtTokenProvider.generateRawRefreshToken()).willReturn("raw-refresh");
    given(jwtTokenProvider.generateAccessToken(any())).willReturn("access.token");
    given(jwtTokenProvider.refreshTokenExpiry()).willReturn(LocalDateTime.now().plusDays(14));

    // when
    TokenResponse result = refreshTokenService.issueTokens(user);

    // then
    assertThat(result.accessToken()).isEqualTo("access.token");
    assertThat(result.refreshToken()).isEqualTo("raw-refresh");
    verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
  }

  @Test
  void givenValidRawToken_whenRotate_thenDeletesOldAndIssuesNew() {
    // given
    String rawToken = "old-raw-token";
    String hash = sha256(rawToken);
    RefreshTokenEntity existing = RefreshTokenEntity.of(user, hash, LocalDateTime.now().plusDays(14));
    given(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(eq(hash), any()))
        .willReturn(Optional.of(existing));
    given(jwtTokenProvider.generateRawRefreshToken()).willReturn("new-raw");
    given(jwtTokenProvider.generateAccessToken(any())).willReturn("new.access");
    given(jwtTokenProvider.refreshTokenExpiry()).willReturn(LocalDateTime.now().plusDays(14));

    // when
    TokenResponse result = refreshTokenService.rotate(rawToken);

    // then
    verify(refreshTokenRepository).delete(existing);
    verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    assertThat(result.refreshToken()).isEqualTo("new-raw");
    assertThat(result.accessToken()).isEqualTo("new.access");
  }

  @Test
  void givenUnknownToken_whenRotate_thenThrowsInvalidToken() {
    // given
    given(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(any(), any()))
        .willReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void givenRawToken_whenRevoke_thenDeletesByHash() {
    // given
    String rawToken = "revoke-me";
    String hash = sha256(rawToken);

    // when
    refreshTokenService.revoke(rawToken);

    // then
    verify(refreshTokenRepository).deleteByTokenHash(hash);
  }

  private String sha256(String input) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
