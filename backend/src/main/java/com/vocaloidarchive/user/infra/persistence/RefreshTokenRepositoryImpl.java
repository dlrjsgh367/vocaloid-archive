package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpa;
  private final UserJpaRepository userJpa;
  private final RefreshTokenDomainMapper mapper;

  @Override
  public RefreshToken save(RefreshToken token) {
    UserEntity userRef = userJpa.getReferenceById(token.getUserId());
    RefreshTokenEntity saved = jpa.save(RefreshTokenEntityMapper.toNewEntity(token, userRef));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<RefreshToken> findValidByTokenHash(String tokenHash, LocalDateTime now) {
    return jpa.findByTokenHashAndExpiresAtAfter(tokenHash, now)
        .map(mapper::toDomain);
  }

  @Override
  public void deleteByTokenHash(String tokenHash) {
    jpa.deleteByTokenHash(tokenHash);
  }

  @Override
  public void deleteById(Long id) {
    jpa.deleteById(id);
  }
}
