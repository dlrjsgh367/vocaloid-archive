package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.infra.persistence.RefreshTokenEntity;
import com.vocaloidarchive.user.infra.persistence.RefreshTokenJpaRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class RefreshTokenRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired UserJpaRepository userRepository;
  @Autowired RefreshTokenJpaRepository refreshTokenRepository;

  private UserEntity user;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    user = userRepository.save(UserEntity.of("rtuser", "rt@test.com", "hash"));
  }

  @Test
  void givenNonExpiredToken_whenFindByHashAndNotExpired_thenReturnsToken() {
    LocalDateTime future = LocalDateTime.now().plusDays(14);
    refreshTokenRepository.save(RefreshTokenEntity.of(user, "hash-abc", future));

    Optional<RefreshTokenEntity> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-abc", LocalDateTime.now());

    assertThat(result).isPresent();
    assertThat(result.get().getTokenHash()).isEqualTo("hash-abc");
  }

  @Test
  void givenExpiredToken_whenFindByHashAndNotExpired_thenReturnsEmpty() {
    LocalDateTime past = LocalDateTime.now().minusDays(1);
    refreshTokenRepository.save(RefreshTokenEntity.of(user, "hash-expired", past));

    Optional<RefreshTokenEntity> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-expired", LocalDateTime.now());

    assertThat(result).isEmpty();
  }

  @Test
  void givenExistingToken_whenDeleteByHash_thenTokenIsRemoved() {
    refreshTokenRepository.save(
        RefreshTokenEntity.of(user, "hash-del", LocalDateTime.now().plusDays(1)));

    refreshTokenRepository.deleteByTokenHash("hash-del");

    assertThat(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(
        "hash-del", LocalDateTime.now())).isEmpty();
  }

  @Test
  void givenNonExistentHash_whenDeleteByHash_thenNoError() {
    refreshTokenRepository.deleteByTokenHash("nonexistent-hash");
  }
}
