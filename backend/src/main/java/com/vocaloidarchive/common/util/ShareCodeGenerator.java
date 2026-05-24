package com.vocaloidarchive.common.util;

import java.security.SecureRandom;

/**
 * Generates short, URL-safe share codes for public playlist links (e.g. {@code /p/{code}}).
 * 10 chars of base62 → ~10^17 space, unguessable. Uniqueness is enforced by the DB UNIQUE
 * constraint; callers retry on collision.
 */
public final class ShareCodeGenerator {

  private static final char[] ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int LEN = 10;
  private static final SecureRandom RNG = new SecureRandom();

  private ShareCodeGenerator() {}

  public static String generate() {
    StringBuilder sb = new StringBuilder(LEN);
    for (int i = 0; i < LEN; i++) {
      sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
    }
    return sb.toString();
  }
}
