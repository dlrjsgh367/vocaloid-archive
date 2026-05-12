package com.vocaloidarchive.user.application.port;

import com.vocaloidarchive.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {
  RefreshToken save(RefreshToken token);
  Optional<RefreshToken> findValidByTokenHash(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
  void deleteById(Long id);
}
