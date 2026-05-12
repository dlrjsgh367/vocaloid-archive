# Phase 7-1 — User Domain Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `user` 도메인을 `ARCHITECTURE.md` 4-layer (domain/application/infra/interfaces) 구조로 마이그레이션하고, User Pure POJO 와 UserEntity 를 분리한다.

**Architecture:** Pure POJO `domain.User` + `infra.persistence.UserEntity` 분리, UseCase per-method (SignUp / Authenticate / IssueTokens / Rotate / Revoke), Port-Adapter 패턴 (`application.port.UserRepository` interface ↔ `infra.persistence.UserRepositoryImpl`). 다른 도메인 엔티티의 ManyToOne 타깃을 `User` → `UserEntity` 로 mechanical rename.

**Tech Stack:** Java 17, Spring Boot 3.2.5, JPA, BCrypt, JJWT, JUnit 5 (Disabled 처리 일부)

**전제 조건:**
- spec 문서: `docs/superpowers/specs/2026-05-12-phase-7-architecture-migration.md` 검토 완료
- worktree: `.worktrees/phase-7-1-user-migration` 에서 작업 (branch: `feature/phase-7-1-user-migration`)
- 베이스: `main` (커밋 `c69c623` 이후)

**완료 조건 (DoD):**
- `com.vocaloidarchive.user.{domain,application,infra,interfaces}` 4 패키지만 존재
- `com.vocaloidarchive.user.{controller,service,repository,dto}` 패키지 모두 제거
- `domain/User`, `domain/RefreshToken` 은 JPA 어노테이션 없는 Pure POJO
- 다른 도메인 엔티티는 `UserEntity` 를 ManyToOne 참조
- `./gradlew compileJava compileTestJava` 통과
- 살아있는 테스트는 `./gradlew test` 통과 (마이그레이션 영향 테스트는 `@Disabled("phase-7-migration")` 허용)
- 기존 API 엔드포인트 (`/api/auth/signup|login|refresh|logout`) 동일하게 동작

---

## Task 0: 워크트리 + 브랜치 준비

**Files:** 없음 (git 작업)

- [x] **Step 1: 워크트리 생성**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git worktree add .worktrees/phase-7-1-user-migration -b feature/phase-7-1-user-migration
cd .worktrees/phase-7-1-user-migration
```

- [x] **Step 2: 기본 빌드 통과 확인 (베이스라인)**

```bash
cd backend && ./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

---

## Task 1: User/RefreshToken 엔티티를 infra 로 mechanical rename

이 task 는 빌드 통과를 유지하기 위해 한 번에 모든 import 를 갱신한다. 옛 `domain/User` 와 새 `UserEntity` 가 동시에 @Entity 일 수 없으므로 단일 commit 으로 진행.

**Files:**
- Rename: `user/domain/User.java` → `user/infra/persistence/UserEntity.java`
- Rename: `user/domain/RefreshToken.java` → `user/infra/persistence/RefreshTokenEntity.java`
- Rename: `user/repository/UserRepository.java` → `user/infra/persistence/UserJpaRepository.java`
- Rename: `user/repository/RefreshTokenRepository.java` → `user/infra/persistence/RefreshTokenJpaRepository.java`
- Modify (import + 타입): `song/domain/Song.java`, `comment/domain/Comment.java`, `playlist/domain/Playlist.java`, `like/domain/Like.java`
- Modify (import + 타입): `song/service/SongService.java`, `comment/service/CommentService.java`, `playlist/service/PlaylistService.java`, `like/service/LikeService.java`
- Modify (import + 타입): `song/dto/response/UserSummary.java`, `user/service/UserService.java`, `user/service/RefreshTokenService.java`, `user/dto/response/UserResponse.java`, `user/controller/AuthController.java`, `common/security/CustomUserDetailsService.java`
- Modify (테스트 import): 모든 `*Test.java` 의 `import com.vocaloidarchive.user.domain.User` → `import com.vocaloidarchive.user.infra.persistence.UserEntity`, `import com.vocaloidarchive.user.repository.UserRepository` → `import com.vocaloidarchive.user.infra.persistence.UserJpaRepository`, RefreshToken 도 동일

- [x] **Step 1: `user/infra/persistence/UserEntity.java` 생성**

옛 `User.java` 의 내용을 옮기되, 클래스명만 `UserEntity` 로 변경. `User.of(...)` → `UserEntity.of(...)`.

```java
package com.vocaloidarchive.user.infra.persistence;

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
public class UserEntity {

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

  public static UserEntity of(String username, String email, String passwordHash) {
    UserEntity u = new UserEntity();
    u.username = username;
    u.email = email;
    u.passwordHash = passwordHash;
    return u;
  }
}
```

- [x] **Step 2: 옛 `user/domain/User.java` 삭제**

```powershell
Remove-Item backend/src/main/java/com/vocaloidarchive/user/domain/User.java
```

- [x] **Step 3: `user/infra/persistence/RefreshTokenEntity.java` 생성 + 옛 `RefreshToken.java` 삭제**

옛 `RefreshToken.java` 의 내용을 옮기되 `User` → `UserEntity`, 클래스명 `RefreshTokenEntity`, factory `RefreshTokenEntity.of(UserEntity, String, LocalDateTime)`.

- [x] **Step 4: JpaRepository 이동**

`user/infra/persistence/UserJpaRepository.java`:

```java
package com.vocaloidarchive.user.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByEmail(String email);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);
}
```

`user/infra/persistence/RefreshTokenJpaRepository.java`:

```java
package com.vocaloidarchive.user.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
  Optional<RefreshTokenEntity> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
}
```

옛 `user/repository/UserRepository.java`, `user/repository/RefreshTokenRepository.java` 삭제.

- [x] **Step 5: 다른 도메인 엔티티 4개의 import + ManyToOne 타입 변경**

`song/domain/Song.java`:
- `import com.vocaloidarchive.user.domain.User;` → `import com.vocaloidarchive.user.infra.persistence.UserEntity;`
- `private User registeredBy;` → `private UserEntity registeredBy;`
- `Song.of(User registeredBy, ...)` → `Song.of(UserEntity registeredBy, ...)`

`comment/domain/Comment.java`, `playlist/domain/Playlist.java`, `like/domain/Like.java` 동일 패턴 (`user` 필드).

- [x] **Step 6: 옛 user/service, controller, dto 의 import 갱신 (이 task 종료 시점 빌드 통과 위해)**

`user/service/UserService.java`:
- `import com.vocaloidarchive.user.domain.User;` 제거
- `import com.vocaloidarchive.user.infra.persistence.UserEntity;` 추가
- `import com.vocaloidarchive.user.repository.UserRepository;` → `import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;`
- `private final UserRepository userRepository;` → `private final UserJpaRepository userRepository;`
- 메서드 시그니처 `public User authenticate(...)` → `public UserEntity authenticate(...)`
- `User user = User.of(...)` → `UserEntity user = UserEntity.of(...)`

`user/service/RefreshTokenService.java`, `user/controller/AuthController.java`, `user/dto/response/UserResponse.java`, `common/security/CustomUserDetailsService.java` 도 같은 mechanical 변경.

- [x] **Step 7: 다른 도메인 service/dto 의 import 갱신**

`song/service/SongService.java`, `comment/service/CommentService.java`, `playlist/service/PlaylistService.java`, `like/service/LikeService.java`, `song/dto/response/UserSummary.java`:
- `User` 클래스 사용을 `UserEntity` 로 변경

- [x] **Step 8: 모든 테스트의 import 일괄 갱신**

```bash
# 검색
grep -rn "com.vocaloidarchive.user.domain.User\b" backend/src
grep -rn "com.vocaloidarchive.user.repository.UserRepository" backend/src
```

각 hit 에 대해 mechanical 변경. `User` 변수 타입도 `UserEntity` 로.

- [x] **Step 9: 빌드 통과 확인**

```bash
cd backend && ./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [x] **Step 10: 기존 테스트 통과 확인 (regression 체크)**

```bash
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL — 이 단계에서는 옛 ServiceTest 등이 모두 살아있어야 함. 깨지면 즉시 수정.

- [x] **Step 11: Commit**

```bash
git add backend
git commit -m "refactor(user): rename User entity to UserEntity and move to infra/persistence

Mechanical rename to prepare for Pure POJO domain.User separation.
No behavior change."
```

---

## Task 2: Pure POJO `domain.User` + `domain.RefreshToken` 신설

**Files:**
- Create: `user/domain/User.java` (Pure POJO)
- Create: `user/domain/RefreshToken.java` (Pure POJO)

- [x] **Step 1: `domain/User.java` 생성**

```java
package com.vocaloidarchive.user.domain;

import java.time.LocalDateTime;

public class User {

  private final Long id;
  private final String username;
  private final String email;
  private final String passwordHash;
  private final String profileImageUrl;
  private final LocalDateTime createdAt;

  private User(Long id, String username, String email, String passwordHash,
               String profileImageUrl, LocalDateTime createdAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.profileImageUrl = profileImageUrl;
    this.createdAt = createdAt;
  }

  public static User newSignup(String username, String email, String passwordHash) {
    return new User(null, username, email, passwordHash, null, null);
  }

  public static User reconstitute(Long id, String username, String email, String passwordHash,
                                   String profileImageUrl, LocalDateTime createdAt) {
    return new User(id, username, email, passwordHash, profileImageUrl, createdAt);
  }

  public boolean matchesPassword(String rawPassword, java.util.function.BiPredicate<String, String> matcher) {
    return matcher.test(rawPassword, this.passwordHash);
  }

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getProfileImageUrl() { return profileImageUrl; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [x] **Step 2: `domain/RefreshToken.java` 생성**

```java
package com.vocaloidarchive.user.domain;

import java.time.LocalDateTime;

public class RefreshToken {

  private final Long id;
  private final Long userId;
  private final String tokenHash;
  private final LocalDateTime expiresAt;
  private final LocalDateTime createdAt;

  private RefreshToken(Long id, Long userId, String tokenHash,
                       LocalDateTime expiresAt, LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime expiresAt) {
    return new RefreshToken(null, userId, tokenHash, expiresAt, null);
  }

  public static RefreshToken reconstitute(Long id, Long userId, String tokenHash,
                                          LocalDateTime expiresAt, LocalDateTime createdAt) {
    return new RefreshToken(id, userId, tokenHash, expiresAt, createdAt);
  }

  public boolean isExpired(LocalDateTime now) { return !now.isBefore(expiresAt); }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public String getTokenHash() { return tokenHash; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [x] **Step 3: 빌드 통과 확인**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL — 새 도메인 클래스는 아직 어디서도 참조되지 않음. 옛 코드는 `UserEntity` 사용 중이라 영향 없음.

- [x] **Step 4: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/domain
git commit -m "feat(user): add pure POJO domain User and RefreshToken"
```

---

## Task 3: Port 인터페이스 + Mapper + RepositoryImpl

**Files:**
- Create: `user/application/port/UserRepository.java`
- Create: `user/application/port/UserQueryRepository.java`
- Create: `user/application/port/RefreshTokenRepository.java`
- Create: `user/infra/persistence/UserEntityMapper.java`
- Create: `user/infra/persistence/RefreshTokenEntityMapper.java`
- Create: `user/infra/persistence/UserRepositoryImpl.java`
- Create: `user/infra/persistence/UserQueryRepositoryImpl.java`
- Create: `user/infra/persistence/RefreshTokenRepositoryImpl.java`

- [x] **Step 1: `application/port/UserRepository.java` (Command port)**

```java
package com.vocaloidarchive.user.application.port;

import com.vocaloidarchive.user.domain.User;
import java.util.Optional;

public interface UserRepository {
  User save(User user);
  Optional<User> findByEmail(String email);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);
}
```

- [x] **Step 2: `application/port/UserQueryRepository.java` (Read port)**

```java
package com.vocaloidarchive.user.application.port;

import com.vocaloidarchive.user.application.dto.result.UserSummaryResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserQueryRepository {
  Optional<UserSummaryResult> findSummaryById(Long id);
  List<UserSummaryResult> findSummariesByIds(Set<Long> ids);
}
```

`UserSummaryResult` 는 Task 4 에서 생성.

- [x] **Step 3: `application/port/RefreshTokenRepository.java`**

```java
package com.vocaloidarchive.user.application.port;

import com.vocaloidarchive.user.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {
  RefreshToken save(RefreshToken token);
  Optional<RefreshToken> findValidByTokenHash(String tokenHash, LocalDateTime now);
  void deleteByTokenHash(String tokenHash);
  void deleteById(Long id);
}
```

- [x] **Step 4: `UserEntityMapper`**

```java
package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.User;

final class UserEntityMapper {

  private UserEntityMapper() {}

  static User toDomain(UserEntity e) {
    if (e == null) return null;
    return User.reconstitute(
        e.getId(), e.getUsername(), e.getEmail(), e.getPasswordHash(),
        e.getProfileImageUrl(), e.getCreatedAt());
  }

  static UserEntity toNewEntity(User u) {
    return UserEntity.of(u.getUsername(), u.getEmail(), u.getPasswordHash());
  }
}
```

(Update 시나리오는 현재 도메인에 없으므로 `toNewEntity` 만 필요)

- [x] **Step 5: `RefreshTokenEntityMapper`**

```java
package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.RefreshToken;

final class RefreshTokenEntityMapper {

  private RefreshTokenEntityMapper() {}

  static RefreshToken toDomain(RefreshTokenEntity e) {
    if (e == null) return null;
    return RefreshToken.reconstitute(
        e.getId(), e.getUser().getId(), e.getTokenHash(),
        e.getExpiresAt(), e.getCreatedAt());
  }

  static RefreshTokenEntity toNewEntity(RefreshToken t, UserEntity userRef) {
    return RefreshTokenEntity.of(userRef, t.getTokenHash(), t.getExpiresAt());
  }
}
```

- [x] **Step 6: `UserRepositoryImpl`**

```java
package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository jpa;

  @Override
  public User save(User user) {
    UserEntity saved = jpa.save(UserEntityMapper.toNewEntity(user));
    return UserEntityMapper.toDomain(saved);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpa.findByEmail(email).map(UserEntityMapper::toDomain);
  }

  @Override
  public boolean existsByUsername(String username) {
    return jpa.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpa.existsByEmail(email);
  }
}
```

- [x] **Step 7: `UserQueryRepositoryImpl`**

```java
package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.dto.result.UserSummaryResult;
import com.vocaloidarchive.user.application.port.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

  private final UserJpaRepository jpa;

  @Override
  public Optional<UserSummaryResult> findSummaryById(Long id) {
    return jpa.findById(id).map(e -> new UserSummaryResult(e.getId(), e.getUsername()));
  }

  @Override
  public List<UserSummaryResult> findSummariesByIds(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return jpa.findAllById(ids).stream()
        .map(e -> new UserSummaryResult(e.getId(), e.getUsername()))
        .toList();
  }
}
```

- [x] **Step 8: `RefreshTokenRepositoryImpl`**

```java
package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpa;
  private final UserJpaRepository userJpa;

  @Override
  public RefreshToken save(RefreshToken token) {
    UserEntity userRef = userJpa.getReferenceById(token.getUserId());
    RefreshTokenEntity saved = jpa.save(RefreshTokenEntityMapper.toNewEntity(token, userRef));
    return RefreshTokenEntityMapper.toDomain(saved);
  }

  @Override
  public Optional<RefreshToken> findValidByTokenHash(String tokenHash, LocalDateTime now) {
    return jpa.findByTokenHashAndExpiresAtAfter(tokenHash, now)
        .map(RefreshTokenEntityMapper::toDomain);
  }

  @Override
  public void deleteByTokenHash(String tokenHash) {
    jpa.deleteByTokenHash(tokenHash);
  }

  @Override
  public void deleteById(Long id) {
    jpa.deleteById(id);
  }
}
```

- [x] **Step 9: 빌드 통과 확인**

```bash
cd backend && ./gradlew compileJava
```

Note: `UserSummaryResult` 가 아직 없으면 컴파일 깨짐 → Task 4 에서 함께 처리하거나, Step 2/7 의 `UserSummaryResult` 사용 부분을 일시 주석. 권장: Task 4 의 dto 를 먼저 만든 후 본 task 진행.

- [x] **Step 10: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user
git commit -m "feat(user): add application port interfaces and infra repository impls"
```

---

## Task 4: UseCase DTO (Command / Result)

**Files:**
- Create: `user/application/dto/command/SignUpCommand.java`
- Create: `user/application/dto/command/AuthenticateCommand.java`
- Create: `user/application/dto/result/UserResult.java`
- Create: `user/application/dto/result/UserSummaryResult.java`
- Create: `user/application/dto/result/TokenResult.java`

- [x] **Step 1: Command DTOs**

```java
package com.vocaloidarchive.user.application.dto.command;
public record SignUpCommand(String username, String email, String password) {}
```

```java
package com.vocaloidarchive.user.application.dto.command;
public record AuthenticateCommand(String email, String password) {}
```

- [x] **Step 2: Result DTOs**

```java
package com.vocaloidarchive.user.application.dto.result;

import com.vocaloidarchive.user.domain.User;
import java.time.LocalDateTime;

public record UserResult(Long id, String username, String email, LocalDateTime createdAt) {
  public static UserResult from(User u) {
    return new UserResult(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt());
  }
}
```

```java
package com.vocaloidarchive.user.application.dto.result;
public record UserSummaryResult(Long id, String username) {}
```

```java
package com.vocaloidarchive.user.application.dto.result;
public record TokenResult(String accessToken, String refreshToken) {}
```

- [x] **Step 3: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/application/dto
git commit -m "feat(user): add application DTO (command/result)"
```

---

## Task 5: UserDomainService (공통 helper)

**Files:**
- Create: `user/application/UserDomainService.java`

도메인 로직(공통 hash 등)을 모은다. Facade 부활을 막기 위해 **인프라 호출 없이 순수 변환 / 정책만** 담는다.

- [x] **Step 1: `UserDomainService` 생성**

```java
package com.vocaloidarchive.user.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class UserDomainService {

  public String hashRefreshToken(String rawToken) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
```

- [x] **Step 2: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/user/application/UserDomainService.java
git commit -m "feat(user): add UserDomainService for shared domain helpers"
```

---

## Task 6: UseCase 분해 — SignUp / Authenticate

**Files:**
- Create: `user/application/SignUpUseCase.java`
- Create: `user/application/AuthenticateUseCase.java`

- [x] **Step 1: `SignUpUseCase`**

```java
package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.application.dto.command.SignUpCommand;
import com.vocaloidarchive.user.application.dto.result.UserResult;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpUseCase {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResult invoke(SignUpCommand cmd) {
    if (userRepository.existsByUsername(cmd.username())) {
      throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
    }
    if (userRepository.existsByEmail(cmd.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }
    User saved = userRepository.save(
        User.newSignup(cmd.username(), cmd.email(), passwordEncoder.encode(cmd.password())));
    return UserResult.from(saved);
  }
}
```

- [x] **Step 2: `AuthenticateUseCase`**

```java
package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.application.dto.command.AuthenticateCommand;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticateUseCase {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  public User invoke(AuthenticateCommand cmd) {
    User user = userRepository.findByEmail(cmd.email())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(cmd.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    return user;
  }
}
```

- [x] **Step 3: 빌드 통과 + Commit**

```bash
cd backend && ./gradlew compileJava
git add backend/src/main/java/com/vocaloidarchive/user/application
git commit -m "feat(user): add SignUpUseCase and AuthenticateUseCase"
```

---

## Task 7: UseCase 분해 — IssueTokens / RotateRefreshToken / RevokeRefreshToken

**Files:**
- Create: `user/application/IssueTokensUseCase.java`
- Create: `user/application/RotateRefreshTokenUseCase.java`
- Create: `user/application/RevokeRefreshTokenUseCase.java`

- [x] **Step 1: `IssueTokensUseCase`**

```java
package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.application.dto.result.TokenResult;
import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueTokensUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserDomainService userDomainService;

  @Transactional
  public TokenResult invoke(User user) {
    String raw = jwtTokenProvider.generateRawRefreshToken();
    refreshTokenRepository.save(RefreshToken.issue(
        user.getId(),
        userDomainService.hashRefreshToken(raw),
        jwtTokenProvider.refreshTokenExpiry()));
    String access = jwtTokenProvider.generateAccessToken(user.getId());
    return new TokenResult(access, raw);
  }
}
```

- [x] **Step 2: `RotateRefreshTokenUseCase`**

```java
package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.application.dto.result.TokenResult;
import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.RefreshToken;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RotateRefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final UserDomainService userDomainService;
  private final IssueTokensUseCase issueTokensUseCase;

  @Transactional
  public TokenResult invoke(String rawRefreshToken) {
    String tokenHash = userDomainService.hashRefreshToken(rawRefreshToken);
    RefreshToken existing = refreshTokenRepository
        .findValidByTokenHash(tokenHash, LocalDateTime.now())
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    User user = userRepository.findByEmail(/* placeholder */ "")
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    // NOTE: 위 라인은 의도된 placeholder. UserRepository.findById(Long) 필요 → Step 3 에서 보강.
    refreshTokenRepository.deleteById(existing.getId());
    return issueTokensUseCase.invoke(user);
  }
}
```

- [x] **Step 3: `UserRepository.findById(Long)` 추가**

`application/port/UserRepository.java` 에 메서드 추가:

```java
Optional<User> findById(Long id);
```

`UserRepositoryImpl` 에 구현 추가:

```java
@Override
public Optional<User> findById(Long id) {
  return jpa.findById(id).map(UserEntityMapper::toDomain);
}
```

`RotateRefreshTokenUseCase.invoke` 의 placeholder 를 교체:

```java
User user = userRepository.findById(existing.getUserId())
    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
```

- [x] **Step 4: `RevokeRefreshTokenUseCase`**

```java
package com.vocaloidarchive.user.application;

import com.vocaloidarchive.user.application.port.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevokeRefreshTokenUseCase {

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserDomainService userDomainService;

  @Transactional
  public void invoke(String rawRefreshToken) {
    refreshTokenRepository.deleteByTokenHash(userDomainService.hashRefreshToken(rawRefreshToken));
  }
}
```

- [x] **Step 5: 빌드 통과 + Commit**

```bash
cd backend && ./gradlew compileJava
git add backend/src/main/java/com/vocaloidarchive/user
git commit -m "feat(user): add token UseCases (issue/rotate/revoke)"
```

---

## Task 8: 새 `interfaces/AuthController` + 요청/응답 DTO 이동

**Files:**
- Move: `user/dto/request/SignUpRequest.java` → `user/interfaces/dto/request/SignUpRequest.java`
- Move: `user/dto/request/LoginRequest.java` → `user/interfaces/dto/request/LoginRequest.java`
- Move: `user/dto/request/RefreshRequest.java` → `user/interfaces/dto/request/RefreshRequest.java`
- Move: `user/dto/response/UserResponse.java` → `user/interfaces/dto/response/UserResponse.java`
- Move: `user/dto/response/TokenResponse.java` → `user/interfaces/dto/response/TokenResponse.java`
- Create: `user/interfaces/AuthController.java`
- Delete: `user/controller/AuthController.java`

- [x] **Step 1: request DTO 이동 (패키지명만 변경)**

`user/interfaces/dto/request/SignUpRequest.java`, `LoginRequest.java`, `RefreshRequest.java` 신규 생성 (옛 파일 내용 복사, `package` 변경). 옛 파일은 Task 9 에서 일괄 삭제.

- [x] **Step 2: response DTO 이동**

`UserResponse` 는 `domain.User` 로부터 변환하도록 변경:

```java
package com.vocaloidarchive.user.interfaces.dto.response;

import com.vocaloidarchive.user.application.dto.result.UserResult;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {
  public static UserResponse from(UserResult r) {
    return new UserResponse(r.id(), r.username(), r.email(), r.createdAt());
  }
}
```

`TokenResponse` 도 `TokenResult` 로부터 변환:

```java
package com.vocaloidarchive.user.interfaces.dto.response;

import com.vocaloidarchive.user.application.dto.result.TokenResult;

public record TokenResponse(String accessToken, String refreshToken) {
  public static TokenResponse from(TokenResult r) {
    return new TokenResponse(r.accessToken(), r.refreshToken());
  }
}
```

- [x] **Step 3: 새 `interfaces/AuthController`**

```java
package com.vocaloidarchive.user.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.user.application.AuthenticateUseCase;
import com.vocaloidarchive.user.application.IssueTokensUseCase;
import com.vocaloidarchive.user.application.RevokeRefreshTokenUseCase;
import com.vocaloidarchive.user.application.RotateRefreshTokenUseCase;
import com.vocaloidarchive.user.application.SignUpUseCase;
import com.vocaloidarchive.user.application.dto.command.AuthenticateCommand;
import com.vocaloidarchive.user.application.dto.command.SignUpCommand;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.interfaces.dto.request.LoginRequest;
import com.vocaloidarchive.user.interfaces.dto.request.RefreshRequest;
import com.vocaloidarchive.user.interfaces.dto.request.SignUpRequest;
import com.vocaloidarchive.user.interfaces.dto.response.TokenResponse;
import com.vocaloidarchive.user.interfaces.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SignUpUseCase signUpUseCase;
  private final AuthenticateUseCase authenticateUseCase;
  private final IssueTokensUseCase issueTokensUseCase;
  private final RotateRefreshTokenUseCase rotateRefreshTokenUseCase;
  private final RevokeRefreshTokenUseCase revokeRefreshTokenUseCase;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> signUp(@RequestBody @Valid SignUpRequest req) {
    var result = signUpUseCase.invoke(new SignUpCommand(req.username(), req.email(), req.password()));
    return ApiResponse.success(UserResponse.from(result));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
    User user = authenticateUseCase.invoke(new AuthenticateCommand(req.email(), req.password()));
    return ApiResponse.success(TokenResponse.from(issueTokensUseCase.invoke(user)));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
    return ApiResponse.success(TokenResponse.from(rotateRefreshTokenUseCase.invoke(req.refreshToken())));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest req) {
    revokeRefreshTokenUseCase.invoke(req.refreshToken());
    return ApiResponse.success(null);
  }
}
```

- [x] **Step 4: 빌드 통과 (옛 controller 와 새 controller 두 개 공존, RequestMapping 충돌 발생함)**

옛 `user/controller/AuthController.java` 와 새 `user/interfaces/AuthController.java` 가 동일 `/api/auth` 경로 → 충돌. Step 5 에서 옛 controller 삭제.

- [x] **Step 5: 옛 `user/controller/AuthController.java` 삭제 + 빌드**

```powershell
Remove-Item backend/src/main/java/com/vocaloidarchive/user/controller/AuthController.java
```

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [x] **Step 6: Commit**

```bash
git add backend
git commit -m "feat(user): add new interfaces/AuthController on UseCases, remove old controller"
```

---

## Task 9: `common/security/CustomUserDetailsService` 갱신

**Files:**
- Modify: `common/security/CustomUserDetailsService.java`

- [x] **Step 1: port 의존으로 변경**

```java
package com.vocaloidarchive.common.security;

import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
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
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    return new CustomUserDetails(user.getId());
  }
}
```

- [x] **Step 2: 빌드 + Commit**

```bash
cd backend && ./gradlew compileJava
git add backend/src/main/java/com/vocaloidarchive/common/security/CustomUserDetailsService.java
git commit -m "refactor(security): wire CustomUserDetailsService to UserRepository port"
```

---

## Task 10: 옛 user 패키지 잔재 삭제

**Files:**
- Delete: `user/controller/` 전체 (Task 8 에서 AuthController 삭제됨, 폴더 빈 채일 가능성)
- Delete: `user/service/UserService.java`, `RefreshTokenService.java`
- Delete: `user/repository/` 전체 (Task 1 에서 이동됨)
- Delete: `user/dto/request/SignUpRequest.java`, `LoginRequest.java`, `RefreshRequest.java`
- Delete: `user/dto/response/UserResponse.java`, `TokenResponse.java`
- Delete: `user/dto/` 폴더

- [x] **Step 1: 옛 service 삭제**

```powershell
Remove-Item -Recurse backend/src/main/java/com/vocaloidarchive/user/service
Remove-Item -Recurse backend/src/main/java/com/vocaloidarchive/user/dto
Remove-Item -Recurse backend/src/main/java/com/vocaloidarchive/user/controller -ErrorAction SilentlyContinue
Remove-Item -Recurse backend/src/main/java/com/vocaloidarchive/user/repository -ErrorAction SilentlyContinue
```

- [x] **Step 2: 빌드 확인 — 컴파일 에러 발생 예상**

```bash
cd backend && ./gradlew compileJava
```

다른 도메인 service / test 가 옛 `user.service.UserService` 또는 `user.dto.*` 를 import 하면 컴파일 실패.

- [x] **Step 3: 컴파일 에러 해결**

다른 도메인이 `UserService` 를 직접 의존하는 곳은 없어야 한다 (현재 코드 기준). 만약 있다면 해당 도메인의 service 도 일시 수정 (`UserRepository` port 사용으로 변경).

- [x] **Step 4: `compileTestJava` — 테스트 컴파일 실패 처리**

테스트들이 옛 `user.service.UserServiceTest` 등을 가지고 있음.

영향받는 테스트 (예상):
- `user/service/UserServiceTest.java` — 클래스 자체가 사라진 service 를 test → 삭제 또는 `@Disabled` 처리
- `user/service/RefreshTokenServiceTest.java` — 동일
- `user/controller/AuthControllerTest.java` — 옛 controller test → 새 controller 기준으로 일시 비활성화 (`@Disabled("phase-7-migration")`)
- `user/repository/UserRepositoryTest.java`, `RefreshTokenRepositoryTest.java` — 옛 JpaRepository import → 새 `UserJpaRepository` 로 갱신 또는 `@Disabled`

권장: 테스트 클래스 자체에 `@Disabled("phase-7-migration: rewrite in Phase 7-8")` 처리. **클래스 본문 코드는 컴파일 가능해야** 하므로, 사라진 클래스 (UserService, AuthControllerTest의 `userService.signUp` 등) 를 참조하는 import 와 필드만 일시 주석/삭제 + `@Disabled` 추가.

```java
@Disabled("phase-7-migration: rewrite in Phase 7-8")
class UserServiceTest { /* 사라진 의존 mock 필드 모두 주석/삭제 */ }
```

- [x] **Step 5: 빌드 통과 확인**

```bash
cd backend && ./gradlew compileJava compileTestJava
```

Expected: BUILD SUCCESSFUL

- [x] **Step 6: 테스트 실행 — 살아있는 테스트만 통과**

```bash
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL (skipped 다수)

- [x] **Step 7: Commit**

```bash
git add backend
git commit -m "refactor(user): remove legacy controller/service/repository/dto packages"
```

---

## Task 11: 통합 동작 검증

**Files:** 없음 (수동 검증)

- [x] **Step 1: 로컬 DB + 앱 기동**

```bash
docker compose up db -d
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'
```

- [x] **Step 2: 신규 가입 + 로그인 + refresh + logout 끝까지 호출**

별도 터미널에서:

```bash
curl -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" \
  -d '{"username":"phase7test","email":"p7@test.com","password":"Password!1"}'
```

Expected: HTTP 201 + `success: true`

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"p7@test.com","password":"Password!1"}'
```

Expected: HTTP 200 + `data.accessToken`, `data.refreshToken`

```bash
curl -X POST http://localhost:8080/api/auth/refresh -H "Content-Type: application/json" \
  -d '{"refreshToken":"<위 응답의 refreshToken>"}'
```

Expected: HTTP 200 + 새 토큰

```bash
curl -X POST http://localhost:8080/api/auth/logout -H "Content-Type: application/json" \
  -d '{"refreshToken":"<위 응답의 refreshToken>"}'
```

Expected: HTTP 200 + `success: true`

- [x] **Step 3: 앱 종료 후 정리**

bootRun Ctrl+C, `docker compose down` (필요 시).

- [x] **Step 4: 최종 commit (검증 결과만 plan 체크박스에 반영, 코드 변경 없음)**

검증 통과를 plan 의 체크박스로만 마크.

---

## Task 12: Plan 체크박스 마크 + 머지 준비

**Files:**
- Modify: 본 plan 파일 — 모든 체크박스 `- [x]` 로 갱신
- Create: `docs/superpowers/handoff/2026-MM-DD-phase-7-1-user-complete.md` (간단 요약)

- [x] **Step 1: plan 체크박스 일괄 마크**

- [x] **Step 2: handoff 문서 작성**

```markdown
# Phase 7-1 — User Domain Migration 완료

- 브랜치: `feature/phase-7-1-user-migration`
- 베이스: `c69c623`
- 주요 변경:
  - `user/domain/{User, RefreshToken}` Pure POJO 분리
  - `user/infra/persistence/{UserEntity, RefreshTokenEntity, UserJpaRepository, RefreshTokenJpaRepository, *RepositoryImpl, *Mapper}` 신설
  - `user/application/{SignUpUseCase, AuthenticateUseCase, IssueTokensUseCase, RotateRefreshTokenUseCase, RevokeRefreshTokenUseCase, UserDomainService}` UseCase per-method 분해
  - `user/interfaces/AuthController` 신설
  - 다른 도메인 엔티티 4개의 ManyToOne 타깃을 `UserEntity` 로 갱신
  - 옛 `user.{controller, service, repository, dto}` 패키지 제거
- 비활성 테스트: `*Test` 중 user 도메인 관련 4개 (`@Disabled("phase-7-migration")`)
- 다음 Phase: 7-2 (character)
```

- [x] **Step 3: main 으로 merge**

```bash
cd C:/Users/user/workspace/2026/vocaloid-archive
git checkout main
git merge --no-ff feature/phase-7-1-user-migration -m "Merge feature/phase-7-1-user-migration: Phase 7-1 User domain complete"
git worktree remove .worktrees/phase-7-1-user-migration
```

- [x] **Step 4: 최종 확인**

```bash
cd backend && ./gradlew clean compileJava compileTestJava test
```

Expected: BUILD SUCCESSFUL

---

## 부록 — 영향받는 기존 테스트 (참고)

본 Phase 에서 `@Disabled("phase-7-migration")` 처리 예상:

| 테스트 클래스 | 사유 |
|---|---|
| `user/service/UserServiceTest` | 클래스 자체 사라짐 — Phase 7-8 에서 UseCase 별 테스트로 재작성 |
| `user/service/RefreshTokenServiceTest` | 동일 |
| `user/controller/AuthControllerTest` | 옛 controller 사라짐 — 새 controller 기준 재작성 |
| `user/repository/UserRepositoryTest` | port-impl 기준 재작성 필요 |
| `user/repository/RefreshTokenRepositoryTest` | 동일 |
| 다른 도메인 `*Test` 의 `User` 직접 사용 | `UserEntity` 로 갱신 후 살림 (가능한 한 `@Disabled` 회피) |

---
