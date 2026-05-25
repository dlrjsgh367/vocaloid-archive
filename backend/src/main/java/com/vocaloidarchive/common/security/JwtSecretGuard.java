package com.vocaloidarchive.common.security;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup if the built-in development JWT secret is still in use under a
 * non-development profile. Without this, a deploy that forgets to set {@code JWT_SECRET} would boot
 * silently with a publicly known signing key, letting anyone forge access tokens for any user.
 *
 * <p>Constructed eagerly as a bean: throwing here aborts context initialization so the app never
 * serves traffic with the insecure default.
 */
@Component
public class JwtSecretGuard {

  /** Base64 of "please-change-me-vocaloid-archive-256bit-development-default-only". */
  private static final String INSECURE_DEFAULT =
      "cGxlYXNlLWNoYW5nZS1tZS12b2NhbG9pZC1hcmNoaXZlLTI1NmJpdC1kZXZlbG9wbWVudC1kZWZhdWx0LW9ubHk=";

  /** Profiles where the built-in default is tolerated. */
  private static final Set<String> DEV_PROFILES = Set.of("local", "test");

  public JwtSecretGuard(@Value("${app.jwt.secret}") String secret, Environment environment) {
    String[] active = environment.getActiveProfiles();
    boolean devOnly =
        active.length == 0 || Arrays.stream(active).allMatch(DEV_PROFILES::contains);
    if (!devOnly && INSECURE_DEFAULT.equals(secret)) {
      throw new IllegalStateException(
          "app.jwt.secret is still the built-in development default under profile(s) "
              + Arrays.toString(active)
              + ". Set the JWT_SECRET environment variable to a unique Base64-encoded 256-bit "
              + "value (generate with: openssl rand -base64 32).");
    }
  }
}
