package com.vocaloidarchive.user.infra.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

  // @EntityGraph fetches user so the mapper's @Condition guard sees an initialized association
  // and the domain RefreshToken.getUserId() resolves (the rotate flow needs the owner id).
  @EntityGraph(attributePaths = {"user"})
  Optional<RefreshTokenEntity> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

  void deleteByTokenHash(String tokenHash);
}
