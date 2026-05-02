package com.vocaloidarchive.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base for tests that need a real MySQL instance.
 *
 * Uses the Testcontainers "singleton container" pattern: ONE mysql:8.0
 * container per JVM, started in the static initializer and shared across
 * all subclasses. Do NOT add {@code @Testcontainers}/{@code @Container} —
 * the JUnit extension's per-class lifecycle conflicts with multiple
 * {@code @SpringBootTest} subclasses sharing the same static field. The
 * container is reaped by Testcontainers' Ryuk daemon at JVM exit; we never
 * stop it explicitly. {@code @ServiceConnection} only requires the
 * container to be running() at Spring context-load time, which the static
 * initializer guarantees.
 */
@ActiveProfiles("test")
public abstract class AbstractMysqlContainerTest {

  @ServiceConnection
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("vocaloid_archive")
      .withUsername("test")
      .withPassword("test");

  static {
    MYSQL.start();
  }
}
