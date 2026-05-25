package com.vocaloidarchive.user.application.dto.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandEmailNormalizationTest {

  @Test
  void signUpCommand_lowercasesAndTrimsEmail() {
    SignUpCommand cmd = new SignUpCommand("Miku", "  TEST@Gmail.COM ", "pw12345678");
    assertThat(cmd.email()).isEqualTo("test@gmail.com");
  }

  @Test
  void authenticateCommand_lowercasesAndTrimsEmail() {
    AuthenticateCommand cmd = new AuthenticateCommand("Test@Gmail.com", "pw12345678");
    assertThat(cmd.email()).isEqualTo("test@gmail.com");
  }

  @Test
  void signUpCommand_toleratesNullEmail() {
    SignUpCommand cmd = new SignUpCommand("Miku", null, "pw12345678");
    assertThat(cmd.email()).isNull();
  }
}
