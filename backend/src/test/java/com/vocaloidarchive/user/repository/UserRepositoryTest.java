package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class UserRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired UserJpaRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  void givenSavedUser_whenFindByEmail_thenReturnsUser() {
    userRepository.save(UserEntity.of("user1", "user1@test.com", "hash"));

    Optional<UserEntity> result = userRepository.findByEmail("user1@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo("user1");
  }

  @Test
  void givenNonExistentEmail_whenFindByEmail_thenReturnsEmpty() {
    assertThat(userRepository.findByEmail("none@test.com")).isEmpty();
  }

  @Test
  void givenExistingUsername_whenExistsByUsername_thenReturnsTrue() {
    userRepository.save(UserEntity.of("taken", "taken@test.com", "hash"));

    assertThat(userRepository.existsByUsername("taken")).isTrue();
    assertThat(userRepository.existsByUsername("free")).isFalse();
  }

  @Test
  void givenExistingEmail_whenExistsByEmail_thenReturnsTrue() {
    userRepository.save(UserEntity.of("user2", "used@test.com", "hash"));

    assertThat(userRepository.existsByEmail("used@test.com")).isTrue();
    assertThat(userRepository.existsByEmail("free@test.com")).isFalse();
  }
}
