package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.user.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
}
