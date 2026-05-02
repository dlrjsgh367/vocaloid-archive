package com.vocaloidarchive.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

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
