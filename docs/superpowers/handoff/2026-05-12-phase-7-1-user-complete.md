# Phase 7-1 — User Domain Migration 완료

- 브랜치: `feature/phase-7-1-user-migration`
- 베이스: `c69c623` (main)
- 머지 시점: 2026-05-12
- 워크트리: `.worktrees/phase-7-1-user-migration` (머지 후 제거)

---

## 변경 요약

`user` 도메인을 ARCHITECTURE.md 4-layer 구조(`domain / application / infra / interfaces`) + UseCase per-method + Pure POJO 도메인으로 마이그레이션.

### 새 패키지 구조

```
com.vocaloidarchive.user
├── domain/                          # Pure POJO (JPA 무관)
│   ├── User.java
│   └── RefreshToken.java
├── application/
│   ├── SignUpUseCase.java
│   ├── AuthenticateUseCase.java
│   ├── IssueTokensUseCase.java
│   ├── RotateRefreshTokenUseCase.java
│   ├── RevokeRefreshTokenUseCase.java
│   ├── UserDomainService.java       # 공통 helper (hashRefreshToken)
│   ├── port/
│   │   ├── UserRepository.java
│   │   ├── UserQueryRepository.java
│   │   └── RefreshTokenRepository.java
│   └── dto/
│       ├── command/{SignUpCommand, AuthenticateCommand}.java
│       └── result/{UserResult, UserSummaryResult, TokenResult}.java
├── infra/persistence/
│   ├── UserEntity.java              # @Entity (구 user.domain.User)
│   ├── RefreshTokenEntity.java      # @Entity (구 user.domain.RefreshToken)
│   ├── UserJpaRepository.java
│   ├── RefreshTokenJpaRepository.java
│   ├── UserEntityMapper.java        # entity ↔ domain
│   ├── RefreshTokenEntityMapper.java
│   ├── UserRepositoryImpl.java      # implements port.UserRepository
│   ├── UserQueryRepositoryImpl.java
│   └── RefreshTokenRepositoryImpl.java
└── interfaces/
    ├── AuthController.java
    └── dto/
        ├── request/{SignUpRequest, LoginRequest, RefreshRequest}.java
        └── response/{UserResponse, TokenResponse}.java
```

### 옛 패키지 제거

- `user/controller/` — 제거 (`AuthController` 가 `interfaces/` 로 이동)
- `user/service/` — 제거 (UseCase 로 분해)
- `user/repository/` — 제거 (Task 1 에서 `infra/persistence/` 로 이동)
- `user/dto/` — 제거 (`application/dto`, `interfaces/dto` 로 분리)
- `user/domain/{User,RefreshToken}.java` 구 @Entity 본문 — `infra/persistence/*Entity.java` 로 이동, 클래스명 `UserEntity` / `RefreshTokenEntity` 로 변경
- 다른 도메인 4개 엔티티 (`Song`, `Comment`, `Playlist`, `Like`) 의 `@ManyToOne User` 타깃이 `UserEntity` 로 mechanical update

### 공통 인프라

- `common/security/CustomUserDetailsService` — `UserRepository` port 의존으로 변경

---

## 도메인 간 ManyToOne 처리 (절충안)

infra layer 에서는 `Song`, `Comment`, `Playlist`, `Like` 엔티티가 `UserEntity` 를 ManyToOne 으로 참조 유지. domain layer 의 Pure POJO 는 다른 도메인 미참조. 완전한 cross-domain 격리(infra 차원의 FK 해제) 는 Modular Monolith 작업으로 후속 Phase 에 분리. — 자세한 근거는 `docs/superpowers/specs/2026-05-12-phase-7-architecture-migration.md` §2.2 참조.

---

## 비활성 테스트 (Phase 7-8 에서 재작성)

`@Disabled("phase-7-migration: rewrite in Phase 7-8")` 스텁으로 교체:
- `user/service/UserServiceTest.java`
- `user/service/RefreshTokenServiceTest.java`
- `user/controller/AuthControllerTest.java`

(`UserRepositoryTest`, `RefreshTokenRepositoryTest` 는 `UserEntity` / `*JpaRepository` 그대로 사용하므로 계속 활성)

---

## API 동작 검증 (수동)

bootRun + curl 4단계 통과:

| 엔드포인트 | HTTP | 결과 |
|---|---|---|
| `POST /api/auth/signup` | 201 | `success: true`, `data.{id,username,email,createdAt}` |
| `POST /api/auth/login` | 200 | `data.{accessToken, refreshToken}` |
| `POST /api/auth/refresh` | 200 | 새 토큰 페어 발급 |
| `POST /api/auth/logout` (Bearer 포함) | 200 | `success: true` |

응답 shape 변경: `UserResponse` 에서 `profileImageUrl` 필드 제거 (프론트엔드 미사용 확인).

---

## 커밋 9개

```
8c1f62c refactor(user): rename User entity to UserEntity and move to infra/persistence
3bcf8d5 feat(user): add pure POJO domain User and RefreshToken
c572613 feat(user): add application port interfaces, DTOs, and infra repository impls
c499d1e feat(user): add UserDomainService for shared domain helpers
4148822 feat(user): add SignUpUseCase and AuthenticateUseCase
2b19765 feat(user): add token UseCases (issue/rotate/revoke)
22bdeb0 feat(user): add new interfaces/AuthController on UseCases, remove old controller
21b1f1e refactor(security): wire CustomUserDetailsService to UserRepository port
1f31258 refactor(user): remove legacy service/dto packages and disable broken tests
```

---

## 다음 Phase

**Phase 7-2 — Character 도메인** (가장 단순. Read-only 시드 데이터. 패턴 검증용).

이후 7-3 `tag` → 7-4 `like` → 7-5 `comment` → 7-6 `playlist` → 7-7 `song` → 7-8 cleanup (테스트 일괄 재작성).
