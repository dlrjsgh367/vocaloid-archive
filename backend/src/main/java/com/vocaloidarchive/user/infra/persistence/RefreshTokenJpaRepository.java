package com.vocaloidarchive.user.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
  Optional<RefreshTokenEntity> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
}
