package com.vocaloidarchive.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtSecretGuardTest {

  private static final String INSECURE_DEFAULT =
      "cGxlYXNlLWNoYW5nZS1tZS12b2NhbG9pZC1hcmNoaXZlLTI1NmJpdC1kZXZlbG9wbWVudC1kZWZhdWx0LW9ubHk=";
  private static final String CUSTOM_SECRET = "c29tZS1vdGhlci1zZWNyZXQtMjU2LWJpdC12YWx1ZQ==";

  private MockEnvironment envWithProfiles(String... profiles) {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles(profiles);
    return env;
  }

  @Test
  void givenDefaultSecretUnderDockerProfile_whenConstructed_thenThrows() {
    assertThatThrownBy(() -> new JwtSecretGuard(INSECURE_DEFAULT, envWithProfiles("docker")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  void givenDefaultSecretUnderLocalProfile_whenConstructed_thenOk() {
    assertThatCode(() -> new JwtSecretGuard(INSECURE_DEFAULT, envWithProfiles("local")))
        .doesNotThrowAnyException();
  }

  @Test
  void givenDefaultSecretUnderTestProfile_whenConstructed_thenOk() {
    assertThatCode(() -> new JwtSecretGuard(INSECURE_DEFAULT, envWithProfiles("test")))
        .doesNotThrowAnyException();
  }

  @Test
  void givenCustomSecretUnderDockerProfile_whenConstructed_thenOk() {
    assertThatCode(() -> new JwtSecretGuard(CUSTOM_SECRET, envWithProfiles("docker")))
        .doesNotThrowAnyException();
  }
}
