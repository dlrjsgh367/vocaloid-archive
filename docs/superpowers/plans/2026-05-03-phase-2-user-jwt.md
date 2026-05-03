# Phase 2: User + JWT Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement the User domain (signup/login/refresh/logout) with stateless JWT authentication, including JwtAuthenticationFilter wired into Spring Security, and apply all Phase 2 backlog items to GlobalExceptionHandler and SecurityConfig.

**Architecture:** Stateless JWT (HS256). Access token 30 min, refresh token 14 days stored as SHA-256 hash in DB. `JwtAuthenticationFilter` (OncePerRequestFilter) extracts and validates Bearer tokens, setting `CustomUserDetails` into SecurityContext. Token errors are stored as request attributes and delivered by `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler`. `AuthController` orchestrates login by calling `UserService.authenticate()` then `RefreshTokenService.issueTokens()`. No Spring Security `AuthenticationManager` — all auth is manual.

**Tech Stack:** Spring Boot 3.2.5, Java 17, JJWT 0.12.5, Spring Security 6, Lombok, JUnit 5 + Mockito BDD, `@WebMvcTest` + `@DataJpaTest` + Testcontainers MySQL.

**Working directory:** `C:/Users/user/workspace/2026/vocaloid-archive` (Windows). Run Gradle commands from `backend/`.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `user/domain/User.java` | Create | JPA entity, `@CreatedDate` audit |
| `user/domain/RefreshToken.java` | Create | JPA entity, `token_hash` (SHA-256), `expires_at` |
| `user/repository/UserRepository.java` | Create | `findByEmail`, `existsByUsername`, `existsByEmail` |
| `user/repository/RefreshTokenRepository.java` | Create | `findByTokenHashAndExpiresAtAfter`, `deleteByTokenHash` |
| `user/dto/request/SignUpRequest.java` | Create | Bean Validation: username/email/password |
| `user/dto/request/LoginRequest.java` | Create | Bean Validation: email/password |
| `user/dto/request/RefreshRequest.java` | Create | Bean Validation: refreshToken |
| `user/dto/response/UserResponse.java` | Create | Record + `from(User)` factory |
| `user/dto/response/TokenResponse.java` | Create | Record: accessToken, refreshToken |
| `common/response/ApiResponse.java` | Modify | `ErrorBody` gains `details: Map<String,String>` + `@JsonInclude(NON_NULL)` |
| `common/exception/GlobalExceptionHandler.java` | Modify | Extend `ResponseEntityExceptionHandler`; override `handleMethodArgumentNotValid` (Map details); add `BadCredentialsException`, `ExpiredJwtException` handlers; add log.warn to auth/access handlers |
| `common/security/CustomUserDetails.java` | Create | `UserDetails` impl holding `userId: Long` only (no DB on every request) |
| `common/security/CustomUserDetailsService.java` | Create | `UserDetailsService` impl (required by Spring Boot auto-config); loads by email |
| `common/security/JwtTokenProvider.java` | Create | `generateAccessToken`, `generateRawRefreshToken`, `getUserIdFromToken` (throws on invalid/expired), `refreshTokenExpiry()` |
| `common/security/SecurityUtil.java` | Create | `getCurrentUserId()` from SecurityContext |
| `user/service/UserService.java` | Create | `signUp(SignUpRequest)`, `authenticate(email, password) → User` |
| `user/service/RefreshTokenService.java` | Create | `issueTokens(User)`, `rotate(rawToken)`, `revoke(rawToken)`; SHA-256 hashing |
| `common/security/JwtAuthenticationFilter.java` | Create | `OncePerRequestFilter`; sets SecurityContext or stores error attribute |
| `common/security/JwtAuthenticationEntryPoint.java` | Create | 401 response via `ApiResponse.error(...)` |
| `common/security/JwtAccessDeniedHandler.java` | Create | 403 response via `ApiResponse.error(...)` |
| `common/config/SecurityConfig.java` | Modify | Wire filter + entry points; `AbstractHttpConfigurer::disable`; split `requestMatchers` |
| `user/controller/AuthController.java` | Create | `POST /api/auth/{signup,login,refresh,logout}` |
| `common/exception/GlobalExceptionHandlerTest.java` | Modify | Add: malformed JSON → 400; BadCredentials → 401; validation details as Map |
| `common/response/ApiResponseTest.java` | Modify | Add: `errorWithDetails` factory test |
| `user/service/UserServiceTest.java` | Create | BDD Mockito unit tests |
| `user/service/RefreshTokenServiceTest.java` | Create | BDD Mockito unit tests |
| `common/security/JwtTokenProviderTest.java` | Create | Pure unit tests (no Spring) |
| `user/controller/AuthControllerTest.java` | Create | `@WebMvcTest` with real Security filter chain |
| `user/repository/UserRepositoryTest.java` | Create | `@DataJpaTest` + Testcontainers |
| `user/repository/RefreshTokenRepositoryTest.java` | Create | `@DataJpaTest` + Testcontainers |

---

## Task 1: User + RefreshToken Entities

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/domain/User.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/domain/RefreshToken.java`

- [x] **Step 1: Create `User` entity**

```java
package com.vocaloidarchive.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "profile_image_url", length = 500)
  private String profileImageUrl;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public static User of(String username, String email, String passwordHash) {
    User user = new User();
    user.username = username;
    user.email = email;
    user.passwordHash = passwordHash;
    return user;
  }
}
```

- [x] **Step 2: Create `RefreshToken` entity**

```java
package com.vocaloidarchive.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public static RefreshToken of(User user, String tokenHash, LocalDateTime expiresAt) {
    RefreshToken rt = new RefreshToken();
    rt.user = user;
    rt.tokenHash = tokenHash;
    rt.expiresAt = expiresAt;
    return rt;
  }
}
```

- [x] **Step 3: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL (no errors)

- [x] **Step 4: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/domain/
git commit -m "feat(user): add User and RefreshToken JPA entities"
```

---

## Task 2: UserRepository + RefreshTokenRepository

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/repository/UserRepository.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/repository/RefreshTokenRepository.java`

- [x] **Step 1: Create `UserRepository`**

```java
package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);
}
```

- [x] **Step 2: Create `RefreshTokenRepository`**

```java
package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.user.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
}
```

- [x] **Step 3: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL

- [x] **Step 4: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/repository/
git commit -m "feat(user): add UserRepository and RefreshTokenRepository"
```

---

## Task 3: DTOs (Request + Response)

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/dto/request/SignUpRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/dto/request/LoginRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/dto/request/RefreshRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/dto/response/UserResponse.java`
- Create: `backend/src/main/java/com/vocaloidarchive/user/dto/response/TokenResponse.java`

- [x] **Step 1: Create `SignUpRequest`**

```java
package com.vocaloidarchive.user.dto.request;

import jakarta.validation.constraints.*;

public record SignUpRequest(
    @NotBlank
    @Size(min = 3, max = 20, message = "username은 3~20자여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username은 영문, 숫자, 밑줄(_)만 사용 가능합니다")
    String username,

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "email은 100자 이하여야 합니다")
    String email,

    @NotBlank
    @Size(min = 8, max = 72, message = "password는 8~72자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
        message = "password는 영문과 숫자를 혼합하여 8자 이상 입력하세요")
    String password
) {}
```

- [x] **Step 2: Create `LoginRequest`**

```java
package com.vocaloidarchive.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
```

- [x] **Step 3: Create `RefreshRequest`**

```java
package com.vocaloidarchive.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank String refreshToken
) {}
```

- [x] **Step 4: Create `UserResponse`**

```java
package com.vocaloidarchive.user.dto.response;

import com.vocaloidarchive.user.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String profileImageUrl,
    LocalDateTime createdAt
) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getCreatedAt());
  }
}
```

- [x] **Step 5: Create `TokenResponse`**

```java
package com.vocaloidarchive.user.dto.response;

public record TokenResponse(String accessToken, String refreshToken) {}
```

- [x] **Step 6: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL

- [x] **Step 7: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/dto/
git commit -m "feat(user): add auth DTOs (SignUpRequest, LoginRequest, RefreshRequest, UserResponse, TokenResponse)"
```

---

## Task 4: ApiResponse.ErrorBody + GlobalExceptionHandler Refactor

**Files:**
- Modify: `backend/src/main/java/com/vocaloidarchive/common/response/ApiResponse.java`
- Modify: `backend/src/main/java/com/vocaloidarchive/common/exception/GlobalExceptionHandler.java`
- Modify: `backend/src/test/java/com/vocaloidarchive/common/response/ApiResponseTest.java`
- Modify: `backend/src/test/java/com/vocaloidarchive/common/exception/GlobalExceptionHandlerTest.java`

- [x] **Step 1: Write failing tests for new ApiResponse.ErrorBody and GlobalExceptionHandler behavior**

Add to `ApiResponseTest.java` (insert before the closing `}`):

```java
@Test
void errorWithDetails_includesDetailsMap() {
  var details = java.util.Map.of("email", "올바른 이메일 형식이 아닙니다");
  var response = ApiResponse.errorWithDetails("VALIDATION_FAILED", "입력값이 올바르지 않습니다", details);

  assertThat(response.success()).isFalse();
  assertThat(response.error().details()).containsEntry("email", "올바른 이메일 형식이 아닙니다");
}

@Test
void error_withoutDetails_detailsIsNull() {
  var response = ApiResponse.error("SOME_CODE", "some message");
  assertThat(response.error().details()).isNull();
}
```

Add to `GlobalExceptionHandlerTest.java` — new DummyController endpoints and test methods.

Add inside `DummyController`:

```java
@PostMapping("/validate-multi")
ApiResponse<String> validateMulti(@RequestBody @Valid MultiFieldRequest req) {
  return ApiResponse.success("ok");
}

@GetMapping("/bad-credentials")
ApiResponse<Void> badCredentials() {
  throw new org.springframework.security.authentication.BadCredentialsException("bad");
}
```

Add inside `GlobalExceptionHandlerTest`:

```java
record MultiFieldRequest(
    @NotBlank String name,
    @jakarta.validation.constraints.Email String email
) {}

@Test
void givenMalformedJson_whenPosted_thenReturns400WithApiResponse() throws Exception {
  mockMvc.perform(post("/dummy/echo")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{invalid-json"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false));
}

@Test
void givenMultipleValidationErrors_whenPosted_thenDetailsIsMap() throws Exception {
  String body = objectMapper.writeValueAsString(new MultiFieldRequest("", "not-an-email"));
  mockMvc.perform(post("/dummy/validate-multi")
      .contentType(MediaType.APPLICATION_JSON)
      .content(body))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
      .andExpect(jsonPath("$.error.details.name").exists())
      .andExpect(jsonPath("$.error.details.email").exists());
}

@Test
void givenBadCredentialsException_whenThrown_thenReturns401InvalidCredentials() throws Exception {
  mockMvc.perform(get("/dummy/bad-credentials"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
}
```

- [x] **Step 2: Run tests to confirm they fail**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.common.response.ApiResponseTest" --tests "com.vocaloidarchive.common.exception.GlobalExceptionHandlerTest" -q 2>&1 | tail -20
```

Expected: FAILED (compilation errors or test failures — `errorWithDetails` doesn't exist yet)

- [x] **Step 3: Update `ApiResponse.java`**

Replace the entire file content:

```java
package com.vocaloidarchive.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

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
    return new ApiResponse<>(false, null, null, new ErrorBody(code, message, null));
  }

  public static <T> ApiResponse<T> errorWithDetails(
      String code, String message, Map<String, String> details) {
    return new ApiResponse<>(false, null, null, new ErrorBody(code, message, details));
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ErrorBody(String code, String message, Map<String, String> details) {}
}
```

- [x] **Step 4: Replace `GlobalExceptionHandler.java`**

```java
package com.vocaloidarchive.common.exception;

import com.vocaloidarchive.common.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
    ErrorCode code = ex.getErrorCode();
    log.warn("BusinessException: code={}, message={}", code.getCode(), ex.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INVALID_CREDENTIALS.getCode(),
            ErrorCode.INVALID_CREDENTIALS.getMessage()));
  }

  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.EXPIRED_TOKEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.EXPIRED_TOKEN.getCode(),
            ErrorCode.EXPIRED_TOKEN.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.INVALID_TOKEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INVALID_TOKEN.getCode(),
            ErrorCode.INVALID_TOKEN.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }

  // Overrides ResponseEntityExceptionHandler.handleMethodArgumentNotValid
  // to return validation field errors as Map<field, message>
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값",
            (first, second) -> first));
    log.warn("Validation failed: {}", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.errorWithDetails(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            details));
  }

  // Overrides ResponseEntityExceptionHandler.handleExceptionInternal for all other
  // Spring MVC exceptions (HttpMessageNotReadableException, NoResourceFoundException, etc.)
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("Spring MVC exception [{}]: {}", status, ex.getMessage());
    return ResponseEntity.status(status)
        .headers(headers)
        .body(ApiResponse.error(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다"));
  }
}
```

- [x] **Step 5: Run tests to confirm they pass**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.common.response.ApiResponseTest" --tests "com.vocaloidarchive.common.exception.GlobalExceptionHandlerTest" -q 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [x] **Step 6: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/response/ApiResponse.java
git add backend/src/main/java/com/vocaloidarchive/common/exception/GlobalExceptionHandler.java
git add backend/src/test/java/com/vocaloidarchive/common/response/ApiResponseTest.java
git add backend/src/test/java/com/vocaloidarchive/common/exception/GlobalExceptionHandlerTest.java
git commit -m "feat(common): add ErrorBody.details field; refactor GlobalExceptionHandler to extend ResponseEntityExceptionHandler"
```

---

## Task 5: CustomUserDetails + CustomUserDetailsService

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/CustomUserDetails.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/CustomUserDetailsService.java`

- [x] **Step 1: Create `CustomUserDetails`**

Holds `userId` only — no DB lookup per request. The JWT filter creates this directly from the token's subject claim.

```java
package com.vocaloidarchive.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final Long userId;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override public String getPassword() { return null; }
  @Override public String getUsername() { return String.valueOf(userId); }
  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }
}
```

- [x] **Step 2: Create `CustomUserDetailsService`**

Required so Spring Boot auto-configures a `DaoAuthenticationProvider` without complaining about missing `UserDetailsService`. Loads user by email (the `username` parameter is email in this app).

```java
package com.vocaloidarchive.common.security;

import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmail(email)
        .map(user -> new CustomUserDetails(user.getId()))
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
  }
}
```

- [x] **Step 3: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL

- [x] **Step 4: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/security/CustomUserDetails.java
git add backend/src/main/java/com/vocaloidarchive/common/security/CustomUserDetailsService.java
git commit -m "feat(security): add CustomUserDetails and CustomUserDetailsService"
```

---

## Task 6: JwtTokenProvider + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/JwtTokenProvider.java`
- Create: `backend/src/test/java/com/vocaloidarchive/common/security/JwtTokenProviderTest.java`

- [x] **Step 1: Write failing tests**

```java
package com.vocaloidarchive.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

  // 256-bit key encoded in Base64 (required by HS256)
  private static final String SECRET = Base64.getEncoder()
      .encodeToString("test-secret-key-256bit-long-enough-x".getBytes());

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(SECRET, 30, 14);
  }

  @Test
  void givenUserId_whenGenerateAndParse_thenReturnsSameUserId() {
    String token = provider.generateAccessToken(42L);
    assertThat(provider.getUserIdFromToken(token)).isEqualTo(42L);
  }

  @Test
  void givenExpiredToken_whenGetUserId_thenThrowsExpiredJwtException() {
    JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, -1, 14);
    String expiredToken = shortLived.generateAccessToken(1L);
    assertThatThrownBy(() -> provider.getUserIdFromToken(expiredToken))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void givenInvalidToken_whenGetUserId_thenThrowsJwtException() {
    assertThatThrownBy(() -> provider.getUserIdFromToken("not.a.valid.jwt"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void generateRawRefreshToken_returnsDifferentValuesEachCall() {
    String t1 = provider.generateRawRefreshToken();
    String t2 = provider.generateRawRefreshToken();
    assertThat(t1).isNotEqualTo(t2);
  }

  @Test
  void refreshTokenExpiry_isFourteenDaysFromNow() {
    var expiry = provider.refreshTokenExpiry();
    var now = java.time.LocalDateTime.now();
    assertThat(expiry).isAfter(now.plusDays(13));
    assertThat(expiry).isBefore(now.plusDays(15));
  }
}
```

- [x] **Step 2: Run tests to confirm they fail**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.common.security.JwtTokenProviderTest" -q 2>&1 | tail -10
```

Expected: FAILED (class not found)

- [x] **Step 3: Create `JwtTokenProvider`**

```java
package com.vocaloidarchive.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessTokenExpiryMinutes;
  private final long refreshTokenExpiryDays;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-expiry-minutes}") long accessTokenExpiryMinutes,
      @Value("${app.jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    this.refreshTokenExpiryDays = refreshTokenExpiryDays;
  }

  public String generateAccessToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpiryMinutes * 60 * 1000L);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public String generateRawRefreshToken() {
    return UUID.randomUUID().toString();
  }

  public LocalDateTime refreshTokenExpiry() {
    return LocalDateTime.now().plusDays(refreshTokenExpiryDays);
  }

  // Throws ExpiredJwtException if expired, JwtException if otherwise invalid.
  // Callers (JwtAuthenticationFilter) catch these to set the appropriate error code.
  public Long getUserIdFromToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return Long.parseLong(claims.getSubject());
  }
}
```

- [x] **Step 4: Run tests to confirm they pass**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.common.security.JwtTokenProviderTest" -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 5 tests pass.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/security/JwtTokenProvider.java
git add backend/src/test/java/com/vocaloidarchive/common/security/JwtTokenProviderTest.java
git commit -m "feat(security): add JwtTokenProvider with JJWT 0.12.5"
```

---

## Task 7: SecurityUtil

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/SecurityUtil.java`

- [x] **Step 1: Create `SecurityUtil`**

```java
package com.vocaloidarchive.common.security;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

  public Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
    return userDetails.getUserId();
  }
}
```

- [x] **Step 2: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL

- [x] **Step 3: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/security/SecurityUtil.java
git commit -m "feat(security): add SecurityUtil.getCurrentUserId()"
```

---

## Task 8: UserService + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/service/UserService.java`
- Create: `backend/src/test/java/com/vocaloidarchive/user/service/UserServiceTest.java`

- [x] **Step 1: Write failing tests**

```java
package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock UserRepository userRepository;
  @Mock PasswordEncoder passwordEncoder;
  @InjectMocks UserService userService;

  @Test
  void givenValidRequest_whenSignUp_thenSavesUserAndReturnsResponse() {
    // given
    SignUpRequest request = new SignUpRequest("testuser", "test@example.com", "password1");
    given(userRepository.existsByUsername("testuser")).willReturn(false);
    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(passwordEncoder.encode("password1")).willReturn("hashed");
    User saved = User.of("testuser", "test@example.com", "hashed");
    given(userRepository.save(any(User.class))).willReturn(saved);

    // when
    UserResponse response = userService.signUp(request);

    // then
    assertThat(response.username()).isEqualTo("testuser");
    assertThat(response.email()).isEqualTo("test@example.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void givenDuplicateUsername_whenSignUp_thenThrowsDuplicateUsername() {
    // given
    SignUpRequest request = new SignUpRequest("taken", "new@example.com", "password1");
    given(userRepository.existsByUsername("taken")).willReturn(true);

    // when / then
    assertThatThrownBy(() -> userService.signUp(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.DUPLICATE_USERNAME));
  }

  @Test
  void givenDuplicateEmail_whenSignUp_thenThrowsDuplicateEmail() {
    // given
    SignUpRequest request = new SignUpRequest("newuser", "dup@example.com", "password1");
    given(userRepository.existsByUsername("newuser")).willReturn(false);
    given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

    // when / then
    assertThatThrownBy(() -> userService.signUp(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
  }

  @Test
  void givenValidCredentials_whenAuthenticate_thenReturnsUser() {
    // given
    User user = User.of("user", "user@example.com", "hashed");
    given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("password1", "hashed")).willReturn(true);

    // when
    User result = userService.authenticate("user@example.com", "password1");

    // then
    assertThat(result.getEmail()).isEqualTo("user@example.com");
  }

  @Test
  void givenUnknownEmail_whenAuthenticate_thenThrowsInvalidCredentials() {
    // given
    given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> userService.authenticate("no@example.com", "pw"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
  }

  @Test
  void givenWrongPassword_whenAuthenticate_thenThrowsInvalidCredentials() {
    // given
    User user = User.of("user", "user@example.com", "hashed");
    given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

    // when / then
    assertThatThrownBy(() -> userService.authenticate("user@example.com", "wrong"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
  }
}
```

- [x] **Step 2: Run tests to confirm they fail**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.service.UserServiceTest" -q 2>&1 | tail -10
```

Expected: FAILED (class not found)

- [x] **Step 3: Create `UserService`**

```java
package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse signUp(SignUpRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }
    User user = User.of(
        request.username(),
        request.email(),
        passwordEncoder.encode(request.password()));
    userRepository.save(user);
    return UserResponse.from(user);
  }

  public User authenticate(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    return user;
  }
}
```

- [x] **Step 4: Run tests to confirm they pass**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.service.UserServiceTest" -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 6 tests pass.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/service/UserService.java
git add backend/src/test/java/com/vocaloidarchive/user/service/UserServiceTest.java
git commit -m "feat(user): add UserService (signUp, authenticate) with BDD unit tests"
```

---

## Task 9: RefreshTokenService + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/service/RefreshTokenService.java`
- Create: `backend/src/test/java/com/vocaloidarchive/user/service/RefreshTokenServiceTest.java`

- [x] **Step 1: Write failing tests**

```java
package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock RefreshTokenRepository refreshTokenRepository;
  @Mock JwtTokenProvider jwtTokenProvider;
  @InjectMocks RefreshTokenService refreshTokenService;

  private final User user = User.of("user", "user@test.com", "hash");

  @Test
  void givenUser_whenIssueTokens_thenSavesHashAndReturnsTokens() {
    // given
    given(jwtTokenProvider.generateRawRefreshToken()).willReturn("raw-refresh");
    given(jwtTokenProvider.generateAccessToken(any())).willReturn("access.token");
    given(jwtTokenProvider.refreshTokenExpiry()).willReturn(LocalDateTime.now().plusDays(14));

    // when
    TokenResponse result = refreshTokenService.issueTokens(user);

    // then
    assertThat(result.accessToken()).isEqualTo("access.token");
    assertThat(result.refreshToken()).isEqualTo("raw-refresh");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void givenValidRawToken_whenRotate_thenDeletesOldAndIssuesNew() {
    // given
    String rawToken = "old-raw-token";
    String hash = sha256(rawToken);
    RefreshToken existing = RefreshToken.of(user, hash, LocalDateTime.now().plusDays(14));
    given(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(eq(hash), any()))
        .willReturn(Optional.of(existing));
    given(jwtTokenProvider.generateRawRefreshToken()).willReturn("new-raw");
    given(jwtTokenProvider.generateAccessToken(any())).willReturn("new.access");
    given(jwtTokenProvider.refreshTokenExpiry()).willReturn(LocalDateTime.now().plusDays(14));

    // when
    TokenResponse result = refreshTokenService.rotate(rawToken);

    // then
    verify(refreshTokenRepository).delete(existing);
    verify(refreshTokenRepository).save(any(RefreshToken.class));
    assertThat(result.refreshToken()).isEqualTo("new-raw");
    assertThat(result.accessToken()).isEqualTo("new.access");
  }

  @Test
  void givenUnknownToken_whenRotate_thenThrowsInvalidToken() {
    // given
    given(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(any(), any()))
        .willReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN));
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void givenRawToken_whenRevoke_thenDeletesByHash() {
    // given
    String rawToken = "revoke-me";
    String hash = sha256(rawToken);

    // when
    refreshTokenService.revoke(rawToken);

    // then
    verify(refreshTokenRepository).deleteByTokenHash(hash);
  }

  private String sha256(String input) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
```

- [x] **Step 2: Run tests to confirm they fail**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.service.RefreshTokenServiceTest" -q 2>&1 | tail -10
```

Expected: FAILED (class not found)

- [x] **Step 3: Create `RefreshTokenService`**

```java
package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public TokenResponse issueTokens(User user) {
    String rawRefreshToken = jwtTokenProvider.generateRawRefreshToken();
    String tokenHash = hash(rawRefreshToken);
    LocalDateTime expiresAt = jwtTokenProvider.refreshTokenExpiry();
    refreshTokenRepository.save(RefreshToken.of(user, tokenHash, expiresAt));
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
    return new TokenResponse(accessToken, rawRefreshToken);
  }

  @Transactional
  public TokenResponse rotate(String rawRefreshToken) {
    String tokenHash = hash(rawRefreshToken);
    RefreshToken existing = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter(tokenHash, LocalDateTime.now())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    User user = existing.getUser();
    refreshTokenRepository.delete(existing);
    return issueTokens(user);
  }

  @Transactional
  public void revoke(String rawRefreshToken) {
    refreshTokenRepository.deleteByTokenHash(hash(rawRefreshToken));
  }

  private String hash(String token) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
```

- [x] **Step 4: Run tests to confirm they pass**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.service.RefreshTokenServiceTest" -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 4 tests pass.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/service/RefreshTokenService.java
git add backend/src/test/java/com/vocaloidarchive/user/service/RefreshTokenServiceTest.java
git commit -m "feat(user): add RefreshTokenService (issueTokens, rotate, revoke) with BDD unit tests"
```

---

## Task 10: JwtAuthenticationFilter + EntryPoint + AccessDeniedHandler

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/JwtAuthenticationEntryPoint.java`
- Create: `backend/src/main/java/com/vocaloidarchive/common/security/JwtAccessDeniedHandler.java`

- [x] **Step 1: Create `JwtAuthenticationFilter`**

```java
package com.vocaloidarchive.common.security;

import com.vocaloidarchive.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractBearerToken(request);
    if (token != null) {
      try {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        CustomUserDetails userDetails = new CustomUserDetails(userId);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (ExpiredJwtException e) {
        request.setAttribute("jwtError", ErrorCode.EXPIRED_TOKEN);
      } catch (JwtException e) {
        request.setAttribute("jwtError", ErrorCode.INVALID_TOKEN);
      }
    }
    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }
}
```

- [x] **Step 2: Create `JwtAuthenticationEntryPoint`**

```java
package com.vocaloidarchive.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    ErrorCode errorCode = (ErrorCode) request.getAttribute("jwtError");
    if (errorCode == null) {
      errorCode = ErrorCode.INVALID_TOKEN;
    }
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    response.setStatus(errorCode.getHttpStatus().value());
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }
}
```

- [x] **Step 3: Create `JwtAccessDeniedHandler`**

```java
package com.vocaloidarchive.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    response.setStatus(HttpStatus.FORBIDDEN.value());
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
  }
}
```

- [x] **Step 4: Verify compile**

```powershell
cd backend; ./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/security/JwtAuthenticationFilter.java
git add backend/src/main/java/com/vocaloidarchive/common/security/JwtAuthenticationEntryPoint.java
git add backend/src/main/java/com/vocaloidarchive/common/security/JwtAccessDeniedHandler.java
git commit -m "feat(security): add JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler"
```

---

## Task 11: SecurityConfig Update

**Files:**
- Modify: `backend/src/main/java/com/vocaloidarchive/common/config/SecurityConfig.java`

- [x] **Step 1: Replace `SecurityConfig.java`**

```java
package com.vocaloidarchive.common.config;

import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        // CORS rules are delegated to WebConfig.addCorsMappings — see WebConfig.java
        .cors(cors -> {})
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/health").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/songs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/characters").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/songs/*/comments").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/playlists/*").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
```

- [x] **Step 2: Run full test suite to confirm no regressions**

```powershell
cd backend; ./gradlew test -q 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL (existing HealthControllerTest may need `@Import(SecurityConfig.class)` — see Step 3 if it fails)

- [x] **Step 3: Fix HealthControllerTest if it fails**

If `HealthControllerTest` fails because `SecurityConfig` now requires `JwtAuthenticationFilter` and friends, add mocks:

```java
// In HealthControllerTest, add:
@MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
@MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
@MockBean JwtAccessDeniedHandler jwtAccessDeniedHandler;
@MockBean JwtTokenProvider jwtTokenProvider;
@MockBean CustomUserDetailsService customUserDetailsService;
```

Re-run: `./gradlew test -q` — should pass.

- [x] **Step 4: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/common/config/SecurityConfig.java
git add backend/src/test/  # if HealthControllerTest was modified
git commit -m "feat(security): wire JWT filter + entry points into SecurityConfig; apply backlog polish (AbstractHttpConfigurer::disable, split requestMatchers)"
```

---

## Task 12: AuthController + @WebMvcTest

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/user/controller/AuthController.java`
- Create: `backend/src/test/java/com/vocaloidarchive/user/controller/AuthControllerTest.java`

- [x] **Step 1: Write failing tests**

```java
package com.vocaloidarchive.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.request.LoginRequest;
import com.vocaloidarchive.user.dto.request.RefreshRequest;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.service.RefreshTokenService;
import com.vocaloidarchive.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class AuthControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean UserService userService;
  @MockBean RefreshTokenService refreshTokenService;

  private static final String VALID_TOKEN = "valid.jwt.token";

  @BeforeEach
  void setUp() {
    given(jwtTokenProvider.getUserIdFromToken(VALID_TOKEN)).willReturn(1L);
  }

  @Test
  void givenValidSignUpRequest_whenSignUp_thenReturns201() throws Exception {
    SignUpRequest req = new SignUpRequest("testuser", "test@example.com", "password1");
    UserResponse resp = new UserResponse(1L, "testuser", "test@example.com", null, LocalDateTime.now());
    given(userService.signUp(any())).willReturn(resp);

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.username").value("testuser"));
  }

  @Test
  void givenInvalidSignUpRequest_whenSignUp_thenReturns400WithDetailsMap() throws Exception {
    SignUpRequest req = new SignUpRequest("", "not-email", "short");

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.error.details").isMap());
  }

  @Test
  void givenValidLoginRequest_whenLogin_thenReturns200WithTokens() throws Exception {
    LoginRequest req = new LoginRequest("test@example.com", "password1");
    User user = User.of("user", "test@example.com", "hash");
    TokenResponse tokens = new TokenResponse("access.token", "raw-refresh");
    given(userService.authenticate("test@example.com", "password1")).willReturn(user);
    given(refreshTokenService.issueTokens(user)).willReturn(tokens);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("access.token"))
        .andExpect(jsonPath("$.data.refreshToken").value("raw-refresh"));
  }

  @Test
  void givenWrongCredentials_whenLogin_thenReturns401InvalidCredentials() throws Exception {
    LoginRequest req = new LoginRequest("test@example.com", "wrong");
    given(userService.authenticate(anyString(), anyString()))
        .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void givenValidRefreshToken_whenRefresh_thenReturns200WithNewTokens() throws Exception {
    RefreshRequest req = new RefreshRequest("old-refresh-token");
    TokenResponse tokens = new TokenResponse("new.access", "new-refresh");
    given(refreshTokenService.rotate("old-refresh-token")).willReturn(tokens);

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new.access"));
  }

  @Test
  void givenNoBearerToken_whenLogout_thenReturns401() throws Exception {
    RefreshRequest req = new RefreshRequest("some-refresh");

    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  @Test
  void givenValidBearer_whenLogout_thenReturns200() throws Exception {
    RefreshRequest req = new RefreshRequest("some-refresh");
    willDoNothing().given(refreshTokenService).revoke(anyString());

    mockMvc.perform(post("/api/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}
```

- [x] **Step 2: Run tests to confirm they fail**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.controller.AuthControllerTest" -q 2>&1 | tail -10
```

Expected: FAILED (class not found)

- [x] **Step 3: Create `AuthController`**

```java
package com.vocaloidarchive.user.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.user.dto.request.LoginRequest;
import com.vocaloidarchive.user.dto.request.RefreshRequest;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.service.RefreshTokenService;
import com.vocaloidarchive.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final RefreshTokenService refreshTokenService;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> signUp(@RequestBody @Valid SignUpRequest request) {
    return ApiResponse.success(userService.signUp(request));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
    var user = userService.authenticate(request.email(), request.password());
    return ApiResponse.success(refreshTokenService.issueTokens(user));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
    return ApiResponse.success(refreshTokenService.rotate(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
    refreshTokenService.revoke(request.refreshToken());
    return ApiResponse.success(null);
  }
}
```

- [x] **Step 4: Run tests to confirm they pass**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.controller.AuthControllerTest" -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 7 tests pass.

- [x] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/controller/AuthController.java
git add backend/src/test/java/com/vocaloidarchive/user/controller/AuthControllerTest.java
git commit -m "feat(user): add AuthController with @WebMvcTest covering signup/login/refresh/logout"
```

---

## Task 13: Repository Integration Tests

**Files:**
- Create: `backend/src/test/java/com/vocaloidarchive/user/repository/UserRepositoryTest.java`
- Create: `backend/src/test/java/com/vocaloidarchive/user/repository/RefreshTokenRepositoryTest.java`

- [x] **Step 1: Create `UserRepositoryTest`**

```java
package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  void givenSavedUser_whenFindByEmail_thenReturnsUser() {
    userRepository.save(User.of("user1", "user1@test.com", "hash"));

    Optional<User> result = userRepository.findByEmail("user1@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo("user1");
  }

  @Test
  void givenNonExistentEmail_whenFindByEmail_thenReturnsEmpty() {
    assertThat(userRepository.findByEmail("none@test.com")).isEmpty();
  }

  @Test
  void givenExistingUsername_whenExistsByUsername_thenReturnsTrue() {
    userRepository.save(User.of("taken", "taken@test.com", "hash"));

    assertThat(userRepository.existsByUsername("taken")).isTrue();
    assertThat(userRepository.existsByUsername("free")).isFalse();
  }

  @Test
  void givenExistingEmail_whenExistsByEmail_thenReturnsTrue() {
    userRepository.save(User.of("user2", "used@test.com", "hash"));

    assertThat(userRepository.existsByEmail("used@test.com")).isTrue();
    assertThat(userRepository.existsByEmail("free@test.com")).isFalse();
  }
}
```

- [x] **Step 2: Create `RefreshTokenRepositoryTest`**

```java
package com.vocaloidarchive.user.repository;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired UserRepository userRepository;
  @Autowired RefreshTokenRepository refreshTokenRepository;

  private User user;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    user = userRepository.save(User.of("rtuser", "rt@test.com", "hash"));
  }

  @Test
  void givenNonExpiredToken_whenFindByHashAndNotExpired_thenReturnsToken() {
    LocalDateTime future = LocalDateTime.now().plusDays(14);
    refreshTokenRepository.save(RefreshToken.of(user, "hash-abc", future));

    Optional<RefreshToken> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-abc", LocalDateTime.now());

    assertThat(result).isPresent();
    assertThat(result.get().getTokenHash()).isEqualTo("hash-abc");
  }

  @Test
  void givenExpiredToken_whenFindByHashAndNotExpired_thenReturnsEmpty() {
    LocalDateTime past = LocalDateTime.now().minusDays(1);
    refreshTokenRepository.save(RefreshToken.of(user, "hash-expired", past));

    Optional<RefreshToken> result = refreshTokenRepository
        .findByTokenHashAndExpiresAtAfter("hash-expired", LocalDateTime.now());

    assertThat(result).isEmpty();
  }

  @Test
  void givenExistingToken_whenDeleteByHash_thenTokenIsRemoved() {
    refreshTokenRepository.save(
        RefreshToken.of(user, "hash-del", LocalDateTime.now().plusDays(1)));

    refreshTokenRepository.deleteByTokenHash("hash-del");

    assertThat(refreshTokenRepository.findByTokenHashAndExpiresAtAfter(
        "hash-del", LocalDateTime.now())).isEmpty();
  }

  @Test
  void givenNonExistentHash_whenDeleteByHash_thenNoError() {
    // deleteByTokenHash is idempotent — should not throw
    refreshTokenRepository.deleteByTokenHash("nonexistent-hash");
  }
}
```

- [x] **Step 3: Run repository tests**

```powershell
cd backend; ./gradlew test --tests "com.vocaloidarchive.user.repository.*" -q 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, 8 tests pass.

- [x] **Step 4: Run the full test suite**

```powershell
cd backend; ./gradlew test -q 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [x] **Step 5: Commit**

```bash
git add backend/src/test/java/com/vocaloidarchive/user/repository/
git commit -m "test(user): add UserRepository and RefreshTokenRepository @DataJpaTest integration tests"
```

---

## Final Verification

- [x] **Run full build + tests**

```powershell
cd backend; ./gradlew clean build -q 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [x] **Smoke test: start application and call health endpoint**

```powershell
docker compose up db -d
cd backend; ./gradlew bootRun --args='--spring.profiles.active=local' &
# wait ~15s for startup
curl http://localhost:8080/api/health
```

Expected: `{"success":true,"data":{"status":"UP"},...}`

- [x] **Smoke test: signup → login → refresh → logout**

```powershell
# signup
curl -s -X POST http://localhost:8080/api/auth/signup `
  -H "Content-Type: application/json" `
  -d '{"username":"smoketest","email":"smoke@test.com","password":"password1"}'

# login (save accessToken + refreshToken from response)
curl -s -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"smoke@test.com","password":"password1"}'

# refresh (use refreshToken from login response)
curl -s -X POST http://localhost:8080/api/auth/refresh `
  -H "Content-Type: application/json" `
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'

# logout (use accessToken as Bearer, refreshToken in body)
curl -s -X POST http://localhost:8080/api/auth/logout `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

Expected: each call returns `{"success":true,...}`

- [x] **Final commit**

```bash
git add .
git commit -m "feat(phase-2): Phase 2 User+JWT complete — signup/login/refresh/logout with JWT filter and full test coverage"
```
