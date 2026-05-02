# Phase 1: Project Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap the VocaloidArchive monorepo with a runnable Spring Boot 3 backend skeleton (common classes, Flyway-managed MySQL schema with character seed), a Vue 3 + Vite frontend skeleton (router/Pinia/axios wired but stubbed), and a docker-compose stack that boots all three services with the backend health endpoint reachable.

**Architecture:** Monorepo (`backend/`, `frontend/`, `docker-compose.yml`). Backend uses Gradle Groovy DSL, Spring Boot 3.2.x, Java 17, JPA + QueryDSL, Flyway for schema, Testcontainers MySQL for repository/integration tests. Frontend uses Vite + Vue 3 Composition API + Pinia + Vue Router + Axios. JWT internals and domain features come in later phases — Phase 1 only delivers the skeleton + a `/api/health` endpoint to prove the stack works end-to-end.

**Tech Stack:** Spring Boot 3.2.5, Java 17, Gradle Groovy, JPA, QueryDSL 5.0.0:jakarta, Flyway 9+, MySQL 8.0, JJWT 0.12.5 (deps only, used in Phase 2), Lombok, JUnit 5, Mockito, Testcontainers MySQL 1.19.7, Vue 3.4+, Vite 5+, Pinia 2+, Vue Router 4+, Axios 1+, MySQL 8.0, Docker Compose v2.

**Coding Style:** Google Java Style Guide (2-space indent, K&R braces). Test method names use BDD `given/when/then` style.

**Working directory:** `C:/Users/user/workspace/2026/vocaloid-archive` (Windows host). All commands assume PowerShell unless noted.

---

## File Structure (created in this phase)

```
vocaloid-archive/
├── README.md                                                     [Task 1]
├── docker-compose.yml                                            [Task 14]
├── backend/
│   ├── build.gradle                                              [Task 2]
│   ├── settings.gradle                                           [Task 2]
│   ├── gradlew, gradlew.bat, gradle/wrapper/*                    [Task 2]
│   ├── Dockerfile                                                [Task 8]
│   ├── .dockerignore                                             [Task 8]
│   └── src/
│       ├── main/
│       │   ├── java/com/vocaloidarchive/
│       │   │   ├── VocaloidArchiveApplication.java               [Task 2]
│       │   │   ├── common/
│       │   │   │   ├── response/
│       │   │   │   │   ├── ApiResponse.java                      [Task 3]
│       │   │   │   │   └── PageResponse.java                     [Task 3]
│       │   │   │   ├── exception/
│       │   │   │   │   ├── ErrorCode.java                        [Task 4]
│       │   │   │   │   ├── BusinessException.java                [Task 4]
│       │   │   │   │   └── GlobalExceptionHandler.java           [Task 5]
│       │   │   │   └── config/
│       │   │   │       ├── WebConfig.java                        [Task 6]
│       │   │   │       ├── JpaConfig.java                        [Task 7]
│       │   │   │       └── SecurityConfig.java                   [Task 6]
│       │   │   └── health/
│       │   │       └── HealthController.java                     [Task 6]
│       │   └── resources/
│       │       ├── application.yml                               [Task 2]
│       │       ├── application-local.yml                         [Task 2]
│       │       ├── application-docker.yml                        [Task 2]
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql                       [Task 9]
│       │           └── V2__seed_characters.sql                   [Task 10]
│       └── test/
│           ├── java/com/vocaloidarchive/
│           │   ├── common/
│           │   │   ├── response/{ApiResponseTest, PageResponseTest}.java                        [Task 3]
│           │   │   └── exception/{BusinessExceptionTest, GlobalExceptionHandlerTest}.java      [Task 4, 5]
│           │   ├── config/JpaConfigIntegrationTest.java                                        [Task 7]
│           │   ├── health/HealthControllerTest.java                                            [Task 6]
│           │   └── support/AbstractMysqlContainerTest.java                                     [Task 7]
│           └── resources/application-test.yml                                                  [Task 7]
└── frontend/
    ├── package.json                                              [Task 11]
    ├── vite.config.js                                            [Task 11]
    ├── index.html                                                [Task 11]
    ├── Dockerfile                                                [Task 13]
    ├── .dockerignore                                             [Task 13]
    └── src/
        ├── main.js                                               [Task 11]
        ├── App.vue                                               [Task 11]
        ├── assets/styles/global.css                              [Task 11]
        ├── router/index.js                                       [Task 12]
        ├── stores/auth.js                                        [Task 12]
        ├── api/index.js                                          [Task 12]
        └── views/
            ├── HomeView.vue                                      [Task 12]
            ├── SongDetailView.vue                                [Task 12]
            ├── SongCreateView.vue                                [Task 12]
            ├── SearchView.vue                                    [Task 12]
            ├── PlaylistView.vue                                  [Task 12]
            └── auth/{LoginView, SignUpView}.vue                  [Task 12]
```

Notes:
- Domain packages (`user/`, `song/`, etc.) are NOT created in Phase 1. They appear in Phase 2+.
- JWT/security filter classes (`JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails`, `SecurityUtil`) are NOT created in Phase 1. The `SecurityConfig` in Task 6 uses `permitAll()` everywhere as a placeholder; Phase 2 hardens it.
- Frontend components (SongCard, SongFilter, etc.) are NOT created in Phase 1 — only views referenced by routes, with minimal placeholder markup.

---

## Pre-flight (run once, not a task)

Before Task 1, the engineer should verify their environment. If any of these fail, stop and resolve before starting tasks.

```powershell
# Java 17
java -version
# expected: openjdk version "17..." (or 17.x.x)

# Node 20+
node --version
# expected: v20.x or higher

# Docker + Compose v2
docker --version
docker compose version
# expected: Docker Engine running, Compose v2.x
```

If Java 17 isn't installed, install via your preferred method (Temurin, Liberica, etc.) and confirm `JAVA_HOME` points to it.

---

## Task 1: Repo root README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write `README.md`**

```markdown
# VocaloidArchive

보컬로이드 곡 큐레이션 플랫폼. 사용자가 곡을 등록하고, 캐릭터/태그/분위기로 탐색하고,
플레이리스트와 좋아요/댓글로 큐레이션하는 웹 서비스.

## Stack

- Backend: Spring Boot 3, Java 17, JPA, QueryDSL, MySQL 8.0
- Frontend: Vue 3 (Vite), Pinia, Vue Router, Axios
- Infra: Docker Compose

## Quick Start (Docker)

```bash
cp .env.example .env   # fill in secrets
docker compose up --build
```

Then open http://localhost:5173 (frontend) and http://localhost:8080/api/health (backend).

## Local Dev (without Docker)

- Backend: `cd backend && ./gradlew bootRun`
- Frontend: `cd frontend && npm install && npm run dev`
- DB: `docker compose up db` (just MySQL)

## Docs

- Spec: `docs/superpowers/specs/2026-05-03-vocaloid-archive-design.md`
- Plans: `docs/superpowers/plans/`
```

- [ ] **Step 2: Commit**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add README.md
git commit -m "docs: add repo root README"
```

---

## Task 2: Backend Gradle / Spring Boot skeleton + application.yml profiles

**Files:**
- Create: `backend/settings.gradle`
- Create: `backend/build.gradle`
- Create: `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/gradle-wrapper.properties`, `backend/gradle/wrapper/gradle-wrapper.jar` (via `gradle wrapper`)
- Create: `backend/src/main/java/com/vocaloidarchive/VocaloidArchiveApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-local.yml`
- Create: `backend/src/main/resources/application-docker.yml`

- [ ] **Step 1: Generate gradle wrapper**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive
mkdir backend
cd backend
# requires gradle on PATH; if not, install via SDKMAN/scoop or use a Spring Initializr download
gradle wrapper --gradle-version 8.7
```

If `gradle` isn't on PATH, alternative: download the wrapper from Spring Initializr (https://start.spring.io) with the same dependencies and copy `gradle/`, `gradlew`, `gradlew.bat` into `backend/`.

Expected: `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/gradle-wrapper.properties`, `backend/gradle/wrapper/gradle-wrapper.jar` exist.

- [ ] **Step 2: Write `backend/settings.gradle`**

```groovy
rootProject.name = 'vocaloid-archive'
```

- [ ] **Step 3: Write `backend/build.gradle`**

```groovy
plugins {
  id 'java'
  id 'org.springframework.boot' version '3.2.5'
  id 'io.spring.dependency-management' version '1.1.5'
}

group = 'com.vocaloidarchive'
version = '0.0.1-SNAPSHOT'

java {
  sourceCompatibility = '17'
}

repositories {
  mavenCentral()
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-web'
  implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
  implementation 'org.springframework.boot:spring-boot-starter-security'
  implementation 'org.springframework.boot:spring-boot-starter-validation'

  implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
  annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jakarta'
  annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
  annotationProcessor 'jakarta.persistence:jakarta.persistence-api'

  implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
  runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
  runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'

  implementation 'org.flywaydb:flyway-core'
  implementation 'org.flywaydb:flyway-mysql'

  runtimeOnly 'com.mysql:mysql-connector-j'

  compileOnly 'org.projectlombok:lombok'
  annotationProcessor 'org.projectlombok:lombok'

  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testImplementation 'org.springframework.security:spring-security-test'
  testImplementation 'org.testcontainers:junit-jupiter:1.19.7'
  testImplementation 'org.testcontainers:mysql:1.19.7'
}

tasks.named('test') {
  useJUnitPlatform()
}

// QueryDSL Q-class generation
def querydslDir = layout.buildDirectory.dir('generated/querydsl').get().asFile
sourceSets {
  main {
    java {
      srcDirs += querydslDir
    }
  }
}
tasks.withType(JavaCompile).configureEach {
  options.generatedSourceOutputDirectory = querydslDir
}
clean {
  delete querydslDir
}
```

- [ ] **Step 4: Write `backend/src/main/java/com/vocaloidarchive/VocaloidArchiveApplication.java`**

```java
package com.vocaloidarchive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VocaloidArchiveApplication {

  public static void main(String[] args) {
    SpringApplication.run(VocaloidArchiveApplication.class, args);
  }
}
```

- [ ] **Step 5: Write `backend/src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: vocaloid-archive
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQL8Dialect
      hibernate.format_sql: true
    open-in-view: false
  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: 8080

app:
  jwt:
    secret: ${JWT_SECRET:please-change-me-in-prod-with-256bit-random-secret-value}
    access-token-expiry-minutes: 30
    refresh-token-expiry-days: 14
  cors:
    origins: ${APP_CORS_ORIGINS:http://localhost:5173}

logging:
  level:
    com.vocaloidarchive: DEBUG
```

- [ ] **Step 6: Write `backend/src/main/resources/application-local.yml`**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vocaloid_archive?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

- [ ] **Step 7: Write `backend/src/main/resources/application-docker.yml`**

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

- [ ] **Step 8: Verify build (without DB)**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/backend
./gradlew clean compileJava
```

Expected: `BUILD SUCCESSFUL`. (Full `build` with tests will fail until Task 9+ because Flyway needs the schema; we run only `compileJava` here.)

- [ ] **Step 9: Commit**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add backend/build.gradle backend/settings.gradle backend/gradlew backend/gradlew.bat backend/gradle/ backend/src/
git commit -m "build: bootstrap Spring Boot 3 backend with Gradle and profile configs"
```

---

## Task 3: Common response wrappers (`ApiResponse`, `PageResponse`)

These are simple POJOs but TDD-worthy because their JSON shape is the public API contract.

**Files:**
- Test: `backend/src/test/java/com/vocaloidarchive/common/response/ApiResponseTest.java`
- Test: `backend/src/test/java/com/vocaloidarchive/common/response/PageResponseTest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/response/ApiResponse.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/response/PageResponse.java`

- [ ] **Step 1: Write failing test `ApiResponseTest`**

```java
package com.vocaloidarchive.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void givenData_whenSuccess_thenSuccessTrueAndDataPopulated() throws Exception {
    ApiResponse<String> response = ApiResponse.success("hello");

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"success\":true");
    assertThat(json).contains("\"data\":\"hello\"");
    assertThat(json).contains("\"error\":null");
  }

  @Test
  void givenErrorCodeAndMessage_whenError_thenSuccessFalseAndErrorPopulated() throws Exception {
    ApiResponse<Void> response = ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다");

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"success\":false");
    assertThat(json).contains("\"data\":null");
    assertThat(json).contains("\"code\":\"USER_NOT_FOUND\"");
    assertThat(json).contains("\"message\":\"사용자를 찾을 수 없습니다\"");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/backend
./gradlew test --tests ApiResponseTest
```

Expected: FAIL with compilation error (`ApiResponse` doesn't exist).

- [ ] **Step 3: Implement `ApiResponse`**

```java
package com.vocaloidarchive.common.response;

public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    ErrorBody error
) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, data, message, null);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(false, null, null, new ErrorBody(code, message));
  }

  public record ErrorBody(String code, String message) {
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
./gradlew test --tests ApiResponseTest
```

Expected: PASS.

- [ ] **Step 5: Write failing test `PageResponseTest`**

```java
package com.vocaloidarchive.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

  @Test
  void givenSpringPage_whenFrom_thenContentAndMetadataMapped() {
    var springPage = new PageImpl<>(List.of("a", "b", "c"), PageRequest.of(1, 3), 10);

    PageResponse<String> response = PageResponse.from(springPage);

    assertThat(response.content()).containsExactly("a", "b", "c");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(3);
    assertThat(response.totalElements()).isEqualTo(10);
    assertThat(response.totalPages()).isEqualTo(4);
  }
}
```

- [ ] **Step 6: Run test to verify it fails**

```powershell
./gradlew test --tests PageResponseTest
```

Expected: FAIL (`PageResponse` doesn't exist).

- [ ] **Step 7: Implement `PageResponse`**

```java
package com.vocaloidarchive.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages()
    );
  }
}
```

- [ ] **Step 8: Run all tests in this package**

```powershell
./gradlew test --tests "com.vocaloidarchive.common.response.*"
```

Expected: PASS (2 tests).

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/response/ backend/src/test/java/com/vocaloidarchive/common/response/
git commit -m "feat(common): add ApiResponse and PageResponse wrappers"
```

---

## Task 4: `ErrorCode` enum + `BusinessException`

**Files:**
- Test: `backend/src/test/java/com/vocaloidarchive/common/exception/BusinessExceptionTest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/exception/BusinessException.java`

- [ ] **Step 1: Write failing test**

```java
package com.vocaloidarchive.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessExceptionTest {

  @Test
  void givenErrorCode_whenConstructed_thenCarriesErrorCode() {
    BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    assertThat(ex.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
  }

  @Test
  void givenErrorCodeUserNotFound_whenInspected_thenStatusIs404() {
    assertThat(ErrorCode.USER_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ErrorCode.USER_NOT_FOUND.getCode()).isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void givenBusinessException_whenThrown_thenIsRuntimeException() {
    assertThatThrownBy(() -> { throw new BusinessException(ErrorCode.FORBIDDEN); })
        .isInstanceOf(RuntimeException.class);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
./gradlew test --tests BusinessExceptionTest
```

Expected: FAIL (compilation error).

- [ ] **Step 3: Implement `ErrorCode`**

```java
package com.vocaloidarchive.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 4xx
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
  SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "SONG_NOT_FOUND", "곡을 찾을 수 없습니다"),
  CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", "캐릭터를 찾을 수 없습니다"),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다"),
  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND", "플레이리스트를 찾을 수 없습니다"),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 username입니다"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 email입니다"),

  // 5xx
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
```

- [ ] **Step 4: Implement `BusinessException`**

```java
package com.vocaloidarchive.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, String overrideMessage) {
    super(overrideMessage);
    this.errorCode = errorCode;
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

```powershell
./gradlew test --tests BusinessExceptionTest
```

Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/exception/ backend/src/test/java/com/vocaloidarchive/common/exception/
git commit -m "feat(common): add ErrorCode enum and BusinessException"
```

---

## Task 5: `GlobalExceptionHandler`

**Files:**
- Test: `backend/src/test/java/com/vocaloidarchive/common/exception/GlobalExceptionHandlerTest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/exception/GlobalExceptionHandler.java`

We'll exercise the handler via `@WebMvcTest` against a tiny dummy controller. The dummy controller is declared inline in the test class.

- [ ] **Step 1: Write failing test**

```java
package com.vocaloidarchive.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void givenBusinessException_whenThrown_thenReturnsMappedHttpStatusAndErrorBody() throws Exception {
    mockMvc.perform(get("/dummy/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
  }

  @Test
  void givenInvalidPayload_whenPosted_thenReturnsValidationFailed() throws Exception {
    String body = objectMapper.writeValueAsString(new DummyRequest(""));

    mockMvc.perform(post("/dummy/echo").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void givenUnknownException_whenThrown_thenReturnsInternalServerError() throws Exception {
    mockMvc.perform(get("/dummy/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
  }

  @RestController
  @RequestMapping("/dummy")
  static class DummyController {
    @GetMapping("/business-error")
    ApiResponse<Void> businessError() {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    @PostMapping("/echo")
    ApiResponse<String> echo(@RequestBody @Valid DummyRequest req) {
      return ApiResponse.success(req.value());
    }

    @GetMapping("/boom")
    ApiResponse<Void> boom() {
      throw new IllegalStateException("kaboom");
    }
  }

  record DummyRequest(@NotBlank String value) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
./gradlew test --tests GlobalExceptionHandlerTest
```

Expected: FAIL (handler doesn't exist).

- [ ] **Step 3: Implement `GlobalExceptionHandler`**

```java
package com.vocaloidarchive.common.exception;

import com.vocaloidarchive.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
    ErrorCode code = ex.getErrorCode();
    log.warn("BusinessException: code={}, message={}", code.getCode(), ex.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .collect(Collectors.joining(", "));
    log.warn("Validation failed: {}", details);
    return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
        .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.getCode(), details));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
        .body(ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    return ResponseEntity.status(ErrorCode.INVALID_TOKEN.getHttpStatus())
        .body(ApiResponse.error(ErrorCode.INVALID_TOKEN.getCode(), ErrorCode.INVALID_TOKEN.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
./gradlew test --tests GlobalExceptionHandlerTest
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/exception/GlobalExceptionHandler.java backend/src/test/java/com/vocaloidarchive/common/exception/GlobalExceptionHandlerTest.java
git commit -m "feat(common): add GlobalExceptionHandler covering BusinessException, validation, security, fallback"
```

---

## Task 6: `WebConfig` (CORS) + `SecurityConfig` (skeleton, permitAll) + `HealthController`

These three are tightly coupled — the `HealthController` is what we use to verify both CORS and SecurityConfig.

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/config/WebConfig.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/vocaloidarchive/health/HealthController.java`
- Test: `backend/src/test/java/com/vocaloidarchive/health/HealthControllerTest.java`

- [ ] **Step 1: Write failing test `HealthControllerTest`**

```java
package com.vocaloidarchive.health;

import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, WebConfig.class})
@TestPropertySource(properties = "app.cors.origins=http://localhost:5173")
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void givenAnonymous_whenGetHealth_thenReturnsOk() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("UP"));
  }

  @Test
  void givenLocalhostOrigin_whenPreflight_thenAllowed() throws Exception {
    mockMvc.perform(options("/api/health")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
./gradlew test --tests HealthControllerTest
```

Expected: FAIL (controller and configs don't exist).

- [ ] **Step 3: Implement `HealthController`**

```java
package com.vocaloidarchive.health;

import com.vocaloidarchive.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  @GetMapping
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.success(Map.of("status", "UP"));
  }
}
```

- [ ] **Step 4: Implement `WebConfig`**

```java
package com.vocaloidarchive.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${app.cors.origins}")
  private String corsOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = corsOrigins.split(",");
    registry.addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
```

- [ ] **Step 5: Implement `SecurityConfig` (skeleton — Phase 2 hardens)**

```java
package com.vocaloidarchive.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/health", "/api/auth/**").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/songs/**", "/api/characters", "/api/songs/*/comments").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/playlists/*").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

```powershell
./gradlew test --tests HealthControllerTest
```

Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/config/WebConfig.java backend/src/main/java/com/vocaloidarchive/common/config/SecurityConfig.java backend/src/main/java/com/vocaloidarchive/health/HealthController.java backend/src/test/java/com/vocaloidarchive/health/HealthControllerTest.java
git commit -m "feat(security): add SecurityConfig skeleton, WebConfig CORS, and /api/health endpoint"
```

---

## Task 7: `JpaConfig` (QueryDSL `JPAQueryFactory` bean + JPA Auditing) + Testcontainers test base

We need a real DB to verify the `JPAQueryFactory` bean wires up cleanly with the `EntityManager`. We also create the shared Testcontainers base class that Tasks 9-10 will reuse.

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/config/JpaConfig.java`
- Create: `backend/src/test/java/com/vocaloidarchive/support/AbstractMysqlContainerTest.java`
- Test: `backend/src/test/java/com/vocaloidarchive/config/JpaConfigIntegrationTest.java`
- Create: `backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Write `application-test.yml`**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQL8Dialect
  flyway:
    enabled: true

logging:
  level:
    com.vocaloidarchive: DEBUG
    org.flywaydb: INFO
```

- [ ] **Step 2: Write `AbstractMysqlContainerTest`**

```java
package com.vocaloidarchive.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractMysqlContainerTest {

  @Container
  @ServiceConnection
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("vocaloid_archive")
      .withUsername("test")
      .withPassword("test");
}
```

- [ ] **Step 3: Write failing test `JpaConfigIntegrationTest`**

```java
package com.vocaloidarchive.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JpaConfigIntegrationTest extends AbstractMysqlContainerTest {

  @Autowired(required = false) JPAQueryFactory jpaQueryFactory;

  @Test
  void givenSpringContext_whenLoaded_thenJpaQueryFactoryBeanIsRegistered() {
    assertThat(jpaQueryFactory).isNotNull();
  }
}
```

- [ ] **Step 4: Run test to verify it fails**

```powershell
./gradlew test --tests JpaConfigIntegrationTest
```

Expected: FAIL — Flyway will fail because `V1__init_schema.sql` doesn't exist yet, OR the bean is missing. Either failure is fine for now; we'll address Flyway in Task 9.

If the failure is purely "Flyway has no scripts," temporarily create an empty `backend/src/main/resources/db/migration/V0__placeholder.sql` with a single comment `-- placeholder` to let the context boot. **Delete this placeholder in Task 9 before V1 is added** (Flyway will reject migrations being modified after first run, so deletion before initial run is safe in fresh container).

- [ ] **Step 5: Implement `JpaConfig`**

```java
package com.vocaloidarchive.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

  @Bean
  public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
    return new JPAQueryFactory(entityManager);
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

```powershell
./gradlew test --tests JpaConfigIntegrationTest
```

Expected: PASS. (Container boot may take 30-60s on first run while Docker pulls the image.)

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/config/JpaConfig.java backend/src/test/java/com/vocaloidarchive/support/AbstractMysqlContainerTest.java backend/src/test/java/com/vocaloidarchive/config/JpaConfigIntegrationTest.java backend/src/test/resources/application-test.yml
git commit -m "feat(common): add JpaConfig with JPAQueryFactory bean and Testcontainers MySQL base"
```

---

## Task 8: Backend `Dockerfile` + `.dockerignore`

We're writing the Dockerfile now (separate from docker-compose) so backend image can be built standalone before Task 14's full compose stack.

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/.dockerignore`

- [ ] **Step 1: Write `backend/Dockerfile`**

```dockerfile
# Stage 1: build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew clean bootJar --no-daemon -x test

# Stage 2: runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- [ ] **Step 2: Write `backend/.dockerignore`**

```
build/
.gradle/
out/
bin/
.idea/
*.iml
src/test/
```

- [ ] **Step 3: Verify image builds**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/backend
docker build -t vocaloid-archive-backend:dev .
```

Expected: image built. (First build takes a few minutes due to dependency download.)

- [ ] **Step 4: Commit**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add backend/Dockerfile backend/.dockerignore
git commit -m "build(backend): add multi-stage Dockerfile and dockerignore"
```

---

## Task 9: Flyway `V1__init_schema.sql`

Full schema from spec section 4.1, with all FKs and indexes.

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Test: `backend/src/test/java/com/vocaloidarchive/migration/FlywayMigrationTest.java`

If a placeholder `V0__placeholder.sql` was created in Task 7 step 4, delete it now.

- [ ] **Step 1: Write failing test `FlywayMigrationTest`**

```java
package com.vocaloidarchive.migration;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest extends AbstractMysqlContainerTest {

  @Autowired JdbcTemplate jdbc;

  @Test
  void givenFlywayMigrated_whenInspected_thenAllExpectedTablesExist() {
    List<String> tables = jdbc.queryForList(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()",
        String.class);

    assertThat(tables).contains(
        "users", "refresh_tokens", "songs", "characters",
        "song_characters", "tags", "song_tags", "playlists",
        "playlist_songs", "likes", "comments");
  }

  @Test
  void givenSongsTable_whenColumnsInspected_thenMoodIsEnumStringAndPlayCountDefaults() {
    Integer playCount = jdbc.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_NAME='songs' AND COLUMN_NAME='play_count' AND COLUMN_DEFAULT='0'",
        Integer.class);
    assertThat(playCount).isEqualTo(1);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
./gradlew test --tests FlywayMigrationTest
```

Expected: FAIL (V1 doesn't exist yet, or tables missing).

- [ ] **Step 3: Write `V1__init_schema.sql`**

```sql
-- ============================================================
-- VocaloidArchive Schema V1
-- ============================================================

CREATE TABLE users (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  username           VARCHAR(50)  NOT NULL,
  email              VARCHAR(100) NOT NULL,
  password_hash      VARCHAR(255) NOT NULL,
  profile_image_url  VARCHAR(500) NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  TIMESTAMP    NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_refresh_tokens_user_id (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE characters (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(100) NOT NULL,
  color_hex  VARCHAR(7)   NOT NULL,
  image_url  VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_characters_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tags (
  id    BIGINT      NOT NULL AUTO_INCREMENT,
  name  VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE songs (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  registered_by   BIGINT       NOT NULL,
  title           VARCHAR(200) NOT NULL,
  youtube_url     VARCHAR(500) NULL,
  niconico_url    VARCHAR(500) NULL,
  thumbnail_url   VARCHAR(500) NULL,
  bpm             INT          NULL,
  mood            VARCHAR(20)  NOT NULL,
  play_count      INT          NOT NULL DEFAULT 0,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_songs_created_at (created_at),
  KEY ix_songs_play_count (play_count),
  CONSTRAINT fk_songs_registered_by FOREIGN KEY (registered_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE song_characters (
  song_id      BIGINT NOT NULL,
  character_id BIGINT NOT NULL,
  PRIMARY KEY (song_id, character_id),
  KEY ix_song_characters_character_id (character_id),
  CONSTRAINT fk_song_characters_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
  CONSTRAINT fk_song_characters_character FOREIGN KEY (character_id) REFERENCES characters (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE song_tags (
  song_id BIGINT NOT NULL,
  tag_id  BIGINT NOT NULL,
  PRIMARY KEY (song_id, tag_id),
  KEY ix_song_tags_tag_id (tag_id),
  CONSTRAINT fk_song_tags_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE,
  CONSTRAINT fk_song_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlists (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  title       VARCHAR(200) NOT NULL,
  is_public   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_playlists_user_id (user_id),
  CONSTRAINT fk_playlists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_songs (
  playlist_id  BIGINT NOT NULL,
  song_id      BIGINT NOT NULL,
  order_index  INT    NOT NULL,
  PRIMARY KEY (playlist_id, song_id),
  KEY ix_playlist_songs_order (playlist_id, order_index),
  CONSTRAINT fk_playlist_songs_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
  CONSTRAINT fk_playlist_songs_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE likes (
  user_id   BIGINT    NOT NULL,
  song_id   BIGINT    NOT NULL,
  liked_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, song_id),
  KEY ix_likes_song_id (song_id),
  CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_likes_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comments (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  song_id     BIGINT       NOT NULL,
  content     VARCHAR(500) NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_comments_song_created (song_id, created_at),
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_song FOREIGN KEY (song_id) REFERENCES songs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
./gradlew test --tests FlywayMigrationTest
```

Expected: PASS (2 tests). All 11 tables present and default values correct.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V1__init_schema.sql backend/src/test/java/com/vocaloidarchive/migration/FlywayMigrationTest.java
git commit -m "feat(db): add Flyway V1 init schema with 11 tables, FKs, and indexes"
```

---

## Task 10: Flyway `V2__seed_characters.sql`

Seed 10 major Vocaloid characters.

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__seed_characters.sql`
- Test: `backend/src/test/java/com/vocaloidarchive/migration/CharacterSeedTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.vocaloidarchive.migration;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CharacterSeedTest extends AbstractMysqlContainerTest {

  @Autowired JdbcTemplate jdbc;

  @Test
  void givenV2Migrated_whenCharactersCounted_thenAtLeastTenSeeded() {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM characters", Integer.class);
    assertThat(count).isGreaterThanOrEqualTo(10);
  }

  @Test
  void givenSeed_whenMikuLookedUp_thenColorMatchesSignature() {
    String color = jdbc.queryForObject(
        "SELECT color_hex FROM characters WHERE name = ?", String.class, "하츠네 미쿠");
    assertThat(color).isEqualTo("#39C5BB");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
./gradlew test --tests CharacterSeedTest
```

Expected: FAIL (no seed yet — count is 0).

- [ ] **Step 3: Write `V2__seed_characters.sql`**

```sql
-- ============================================================
-- Seed: major Vocaloid characters with signature colors
-- ============================================================

INSERT INTO characters (name, color_hex, image_url) VALUES
  ('하츠네 미쿠',     '#39C5BB', NULL),
  ('카가미네 린',     '#FFE211', NULL),
  ('카가미네 렌',     '#FFC56C', NULL),
  ('메구리네 루카',   '#FFC0CB', NULL),
  ('KAITO',          '#1E90FF', NULL),
  ('MEIKO',          '#E0233F', NULL),
  ('GUMI',           '#94C947', NULL),
  ('IA',             '#E5E5E5', NULL),
  ('카후',           '#FFB6C1', NULL),
  ('츠루마키 마키',   '#FF7B7B', NULL);
```

- [ ] **Step 4: Run test to verify it passes**

```powershell
./gradlew test --tests CharacterSeedTest
```

Expected: PASS (2 tests).

- [ ] **Step 5: Run full backend test suite**

```powershell
./gradlew test
```

Expected: ALL PASS. This confirms the full backend test suite is green before we wire up frontend and compose.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V2__seed_characters.sql backend/src/test/java/com/vocaloidarchive/migration/CharacterSeedTest.java
git commit -m "feat(db): seed 10 major Vocaloid characters with signature colors"
```

---

## Task 11: Frontend Vite + Vue 3 skeleton

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/assets/styles/global.css`

- [ ] **Step 1: Initialize via npm init (manual since we want exact versions)**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive
mkdir frontend
cd frontend
```

- [ ] **Step 2: Write `frontend/package.json`**

```json
{
  "name": "vocaloid-archive-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview --host 0.0.0.0"
  },
  "dependencies": {
    "axios": "^1.7.0",
    "pinia": "^2.2.0",
    "vue": "^3.4.0",
    "vue-router": "^4.4.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.0",
    "vite": "^5.4.0"
  }
}
```

- [ ] **Step 3: Install dependencies**

```powershell
npm install
```

Expected: `node_modules/` populated, `package-lock.json` created.

- [ ] **Step 4: Write `frontend/vite.config.js`**

```js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 5: Write `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" href="/favicon.ico" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>VocaloidArchive</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 6: Write `frontend/src/assets/styles/global.css`**

```css
:root {
  --bg: #FDF8FF;
  --text: #2A2A2A;
}

* { box-sizing: border-box; }
html, body, #app { margin: 0; padding: 0; height: 100%; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: var(--bg);
  color: var(--text);
}
```

- [ ] **Step 7: Write `frontend/src/main.js` (router/pinia wired in Task 12)**

```js
import { createApp } from 'vue';
import App from './App.vue';
import './assets/styles/global.css';

createApp(App).mount('#app');
```

- [ ] **Step 8: Write `frontend/src/App.vue`**

```vue
<template>
  <div class="app">
    <h1>VocaloidArchive</h1>
    <p>Phase 1 skeleton — router and views wire up in Task 12.</p>
  </div>
</template>

<script setup>
</script>

<style scoped>
.app {
  padding: 24px;
}
</style>
```

- [ ] **Step 9: Verify dev server starts**

```powershell
npm run dev
```

Expected: `Local:   http://localhost:5173/`. Open in browser, see "VocaloidArchive" heading. Stop with Ctrl+C.

- [ ] **Step 10: Verify production build**

```powershell
npm run build
```

Expected: `dist/` directory created with `index.html` and assets.

- [ ] **Step 11: Commit (`node_modules/` and `dist/` are gitignored)**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add frontend/package.json frontend/package-lock.json frontend/vite.config.js frontend/index.html frontend/src/main.js frontend/src/App.vue frontend/src/assets/
git commit -m "build(frontend): bootstrap Vue 3 + Vite skeleton with proxy to backend"
```

---

## Task 12: Frontend router + Pinia auth store + axios skeleton + view stubs

**Files:**
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/stores/auth.js`
- Create: `frontend/src/api/index.js`
- Create: `frontend/src/views/HomeView.vue`
- Create: `frontend/src/views/SongDetailView.vue`
- Create: `frontend/src/views/SongCreateView.vue`
- Create: `frontend/src/views/SearchView.vue`
- Create: `frontend/src/views/PlaylistView.vue`
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/SignUpView.vue`
- Modify: `frontend/src/main.js`
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Write `frontend/src/api/index.js` (interceptors stubbed for Phase 2)**

```js
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
});

// Phase 2: attach Authorization header from auth store
api.interceptors.request.use((config) => config);

// Phase 2: handle 401 with refresh-token rotation
api.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);

export default api;
```

- [ ] **Step 2: Write `frontend/src/stores/auth.js` (state only; actions in Phase 2)**

```js
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    accessToken: null,
    refreshToken: localStorage.getItem('refreshToken') || null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.accessToken,
  },
  actions: {
    // Phase 2 will implement: login, logout, refresh, fetchMe
  },
});
```

- [ ] **Step 3: Write each view stub (each is 5 lines of boilerplate)**

`frontend/src/views/HomeView.vue`:
```vue
<template><div class="view"><h2>Home</h2><p>Phase 3 implements song list + filters.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/SongDetailView.vue`:
```vue
<template><div class="view"><h2>Song Detail</h2><p>Phase 3 implements detail + Phase 4 adds comments/likes.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/SongCreateView.vue`:
```vue
<template><div class="view"><h2>Song Create</h2><p>Phase 3 implements registration form.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/SearchView.vue`:
```vue
<template><div class="view"><h2>Search</h2><p>Phase 3 implements search results.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/PlaylistView.vue`:
```vue
<template><div class="view"><h2>Playlist</h2><p>Phase 5 implements playlist UI.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/auth/LoginView.vue`:
```vue
<template><div class="view"><h2>Login</h2><p>Phase 2 implements login form.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

`frontend/src/views/auth/SignUpView.vue`:
```vue
<template><div class="view"><h2>Sign Up</h2><p>Phase 2 implements signup form.</p></div></template>
<script setup></script>
<style scoped>.view { padding: 24px; }</style>
```

- [ ] **Step 4: Write `frontend/src/router/index.js` with auth guard**

```js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/songs/new', name: 'song-create', component: () => import('@/views/SongCreateView.vue'), meta: { requiresAuth: true } },
  { path: '/songs/:id', name: 'song-detail', component: () => import('@/views/SongDetailView.vue') },
  { path: '/search', name: 'search', component: () => import('@/views/SearchView.vue') },
  { path: '/playlists', name: 'playlists', component: () => import('@/views/PlaylistView.vue'), meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue') },
  { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignUpView.vue') },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore();
    if (!auth.isAuthenticated) {
      return { name: 'login', query: { return: to.fullPath } };
    }
  }
});

export default router;
```

- [ ] **Step 5: Add path alias `@` to `vite.config.js`**

Modify `frontend/vite.config.js` to add resolve.alias:

```js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 6: Wire pinia + router into `frontend/src/main.js`**

Replace `frontend/src/main.js` with:

```js
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/styles/global.css';

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount('#app');
```

- [ ] **Step 7: Replace `frontend/src/App.vue` with router-view shell**

```vue
<template>
  <div class="app">
    <header class="app-header">
      <RouterLink to="/">VocaloidArchive</RouterLink>
      <nav>
        <RouterLink to="/search">Search</RouterLink>
        <RouterLink to="/playlists">Playlists</RouterLink>
        <RouterLink to="/login">Login</RouterLink>
      </nav>
    </header>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #eee;
}
.app-header nav a { margin-left: 16px; }
</style>
```

- [ ] **Step 8: Verify dev server boots and routes render**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/frontend
npm run dev
```

Expected: open http://localhost:5173 → "Home" view.
Visit http://localhost:5173/login → "Login" view.
Visit http://localhost:5173/songs/new → redirects to `/login?return=/songs/new` (auth guard).
Visit http://localhost:5173/songs/123 → "Song Detail" view.
Stop server with Ctrl+C.

- [ ] **Step 9: Commit**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add frontend/src/router/ frontend/src/stores/ frontend/src/api/ frontend/src/views/ frontend/src/main.js frontend/src/App.vue frontend/vite.config.js
git commit -m "feat(frontend): wire router, pinia, axios, and view stubs"
```

---

## Task 13: Frontend `Dockerfile` (dev mode for Phase 1)

We use a dev-mode container (`npm run dev`) rather than a multi-stage prod build. Phase 1 just needs the stack to boot; OCI deployment will add the prod stage later.

**Files:**
- Create: `frontend/Dockerfile`
- Create: `frontend/.dockerignore`

- [ ] **Step 1: Write `frontend/Dockerfile`**

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev"]
```

- [ ] **Step 2: Write `frontend/.dockerignore`**

```
node_modules/
dist/
.vite/
.env
.env.*
```

- [ ] **Step 3: Verify image builds**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/frontend
docker build -t vocaloid-archive-frontend:dev .
```

Expected: image built.

- [ ] **Step 4: Commit**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git add frontend/Dockerfile frontend/.dockerignore
git commit -m "build(frontend): add dev-mode Dockerfile"
```

---

## Task 14: `docker-compose.yml` + full stack boot verification

**Files:**
- Create: `docker-compose.yml`
- Modify: `README.md` (already has compose instructions, but verify section accuracy)

- [ ] **Step 1: Write `docker-compose.yml` (matches spec section 8.1)**

```yaml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: vocaloid_archive
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p${MYSQL_ROOT_PASSWORD:-root}"]
      interval: 5s
      retries: 10
      start_period: 30s

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/vocaloid_archive?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
      JWT_SECRET: ${JWT_SECRET:-please-change-me-in-prod-with-256bit-random-secret-value}
      APP_CORS_ORIGINS: http://localhost:5173

  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
    depends_on:
      - backend
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api
    volumes:
      - ./frontend/src:/app/src
      - ./frontend/index.html:/app/index.html
      - ./frontend/vite.config.js:/app/vite.config.js

volumes:
  db_data:
```

- [ ] **Step 2: Boot the full stack**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive
copy .env.example .env
# (edit .env with real secrets if desired; defaults work for local boot)
docker compose up --build -d
```

Expected: all 3 containers running.

- [ ] **Step 3: Verify backend health endpoint via the running stack**

```powershell
# Wait ~30s for Spring Boot to start, then:
curl http://localhost:8080/api/health
```

Expected response:
```json
{"success":true,"data":{"status":"UP"},"message":null,"error":null}
```

- [ ] **Step 4: Verify frontend renders**

Open http://localhost:5173 in browser. Expected: "VocaloidArchive" header + "Home" view.

- [ ] **Step 5: Verify Flyway migrations ran inside the container DB**

```powershell
docker compose exec db mysql -uroot -proot -e "USE vocaloid_archive; SHOW TABLES; SELECT COUNT(*) FROM characters;"
```

Expected: 11 tables listed (`users`, `refresh_tokens`, `songs`, `characters`, `song_characters`, `tags`, `song_tags`, `playlists`, `playlist_songs`, `likes`, `comments`, plus `flyway_schema_history`), and characters count >= 10.

- [ ] **Step 6: Tear down**

```powershell
docker compose down
```

- [ ] **Step 7: Commit**

```bash
git add docker-compose.yml
git commit -m "build: add docker-compose stack for db + backend + frontend"
```

- [ ] **Step 8: Final verification — fresh clone simulation**

```powershell
cd C:/Users/user/workspace/2026/vocaloid-archive/backend
./gradlew clean test
```

Expected: BUILD SUCCESSFUL with all backend tests passing (target: ApiResponse, PageResponse, BusinessException, GlobalExceptionHandler, HealthController, JpaConfig, FlywayMigration, CharacterSeed = 8 test classes, ~14 tests).

- [ ] **Step 9: Phase 1 complete summary commit (optional)**

If anything was patched up in Step 8 retry, commit those fixups now under a single commit message:

```bash
git add -A  # only run if Step 8 produced fixups
git commit -m "fix: address final phase 1 verification issues"  # only if needed
```

---

## Phase 1 Done Criteria

After Task 14 completes:

- [ ] All backend tests pass (`./gradlew clean test` → BUILD SUCCESSFUL)
- [ ] `docker compose up --build` boots all 3 containers
- [ ] `GET http://localhost:8080/api/health` returns `{"success":true,"data":{"status":"UP"}}`
- [ ] http://localhost:5173 loads the SPA shell with router-view rendering HomeView
- [ ] http://localhost:5173/songs/new redirects to `/login?return=/songs/new`
- [ ] `flyway_schema_history` shows V1 and V2 applied
- [ ] `SELECT COUNT(*) FROM characters` returns >= 10
- [ ] `git log --oneline` shows ~14 small commits matching the task list

When all boxes check, Phase 1 is done. Phase 2 plan (User domain + JWT) is the next deliverable.

---

## Notes for Phase 2 (do not implement now)

These items intentionally deferred from Phase 1 — record here so the engineer knows where they go:

- `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails`, `SecurityUtil` (in `common/security/`) — wired into `SecurityConfig` to replace its current permitAll posture for protected routes.
- `User` and `RefreshToken` entities, `UserService`, `RefreshTokenService`, `UserController`, all DTOs.
- Frontend `auth.js` actions (login/logout/refresh/fetchMe), `api/index.js` real interceptors, `LoginView` / `SignUpView` forms.
- `YoutubeUtil` (used in Phase 3 by `SongService.create`).
