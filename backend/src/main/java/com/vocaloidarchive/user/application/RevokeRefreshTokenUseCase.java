package com.vocaloidarchive.user.application;

import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeRefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserDomainService userDomainService;

  @Transactional
  public void invoke(String rawRefreshToken) {
    refreshTokenRepository.deleteByTokenHash(userDomainService.hashRefreshToken(rawRefreshToken));
  }
}
