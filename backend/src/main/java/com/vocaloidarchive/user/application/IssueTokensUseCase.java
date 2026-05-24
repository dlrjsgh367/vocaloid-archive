package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.application.dto.result.TokenResult;
import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueTokensUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserDomainService userDomainService;

  @Transactional
  public TokenResult invoke(User user) {
    String raw = jwtTokenProvider.generateRawRefreshToken();
    refreshTokenRepository.save(RefreshToken.issue(
        user,
        userDomainService.hashRefreshToken(raw),
        jwtTokenProvider.refreshTokenExpiry()));
    String access = jwtTokenProvider.generateAccessToken(user.getId());
    return new TokenResult(access, raw);
  }
}
