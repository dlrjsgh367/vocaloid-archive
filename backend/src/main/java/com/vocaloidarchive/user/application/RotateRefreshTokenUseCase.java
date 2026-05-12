package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.application.dto.result.TokenResult;
import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RotateRefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final UserDomainService userDomainService;
  private final IssueTokensUseCase issueTokensUseCase;

  @Transactional
  public TokenResult invoke(String rawRefreshToken) {
    String tokenHash = userDomainService.hashRefreshToken(rawRefreshToken);
    RefreshToken existing = refreshTokenRepository
        .findValidByTokenHash(tokenHash, LocalDateTime.now())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    User user = userRepository.findById(existing.getUserId())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    refreshTokenRepository.deleteById(existing.getId());
    return issueTokensUseCase.invoke(user);
  }
}
