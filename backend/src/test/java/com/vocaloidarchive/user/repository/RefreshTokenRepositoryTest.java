package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
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

  @Autowired UserRepository userRepository;
  @Autowired RefreshTokenRepository refreshTokenRepository;

  private User user;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    user = userRepository.save(User.of("rtuser", "rt@test.com", "hash"));
  }

  @Test
  void givenNonExpiredToken_whenFindByHashAndNotExpired_thenReturnsToken() {
    LocalDateTime future = LocalDateTime.now().plusDays(14);
    refreshTokenRepository.save(RefreshToken.of(user, "hash-abc", future));

    Optional<RefreshToken> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-abc", LocalDateTime.now());

    assertThat(result).isPresent();
    assertThat(result.get().getTokenHash()).isEqualTo("hash-abc");
  }

  @Test
  void givenExpiredToken_whenFindByHashAndNotExpired_thenReturnsEmpty() {
    LocalDateTime past = LocalDateTime.now().minusDays(1);
    refreshTokenRepository.save(RefreshToken.of(user, "hash-expired", past));

    Optional<RefreshToken> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-expired", LocalDateTime.now());

    assertThat(result).isEmpty();
  }

  @Test
  void givenExistingToken_whenDeleteByHash_thenTokenIsRemoved() {
    refreshTokenRepository.save(
        RefreshToken.of(user, "hash-del", LocalDateTime.now().plusDays(1)));

    refreshTokenRepository.deleteByTokenHash("hash-del");

    assertThat(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(
        "hash-del", LocalDateTime.now())).isEmpty();
  }

  @Test
  void givenNonExistentHash_whenDeleteByHash_thenNoError() {
    refreshTokenRepository.deleteByTokenHash("nonexistent-hash");
  }
}
