package com.vocaloidarchive.user.application.dto.command;

import java.util.Locale;

public record AuthenticateCommand(String email, String password) {

  // Match SignUpCommand's normalization so login resolves the same row regardless of email casing.
  public AuthenticateCommand {
    email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
