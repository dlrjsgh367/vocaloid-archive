package com.vocaloidarchive.user.application.dto.command;

import java.util.Locale;

public record SignUpCommand(String username, String email, String password) {

  // Canonicalize email to lowercase so casing variants ("A@x.com" / "a@x.com") map to one account,
  // independent of the database collation. Applied at construction so every caller is consistent.
  public SignUpCommand {
    email = normalizeEmail(email);
  }

  private static String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
