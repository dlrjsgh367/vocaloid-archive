# Phase 7 — Architecture Migration Spec

> 작성일: 2026-05-12
> 근거 문서: `C:\Users\user\Downloads\ARCHITECTURE.md`
> 결정자: 이건호 (단독 프로젝트)

---

## 1. 배경

현재 백엔드는 전통적인 Spring 4-layer 구조 (`controller / service / repository / domain / dto`) 로 작성되어 있다.
`ARCHITECTURE.md` 는 도메인 단위 패키지 + Presentation/UseCase/Infra/Domain 4-layer 구조 + UseCase per-method (Vertical Slice) 를 요구한다.

핵심 차이:

| 항목 | 현재 | 목표 |
|---|---|---|
| 패키지 | `controller/service/repository/domain/dto` | `interfaces/application/infra/domain` |
| 도메인 객체 | `@Entity` 가 `domain/` 에 있음 (JPA 결합) | Pure POJO + `infra/XxxEntity` 분리 |
| UseCase | `XxxService` 한 클래스가 메서드 다수 보유 | UseCase 당 클래스 1개 (SRP) |
| Read vs Command | 동일 흐름 | Read 는 Domain bypass |
| Repository | `JpaRepository` 직접 사용 | `Repository (port) + JpaRepository + Impl` 3분할 |
| 도메인 간 결합 | `Song.registeredBy: User` 등 ManyToOne 직접 참조 | `userId: Long` 만 보관 (ID 참조) |

---

## 2. 마이그레이션 전략

### 2.1 핵심 원칙

1. **점진 마이그레이션** — 한 Phase 마다 빌드 + 기존 테스트 통과 + commit. 거대 PR 금지.
2. **테스트 후순위** — 도메인 마이그레이션 동안 깨지는 기존 테스트는 일시적으로 비활성화 (`@Disabled`) 허용. Phase 7-8 에서 일괄 재작성.
3. **점진적 도메인 경계 정립** — 도메인 간 ManyToOne 을 ID 참조로 먼저 끊고, 그 다음 각 도메인 내부 마이그레이션.
4. **하위 호환 hack 금지** — 마이그레이션이 끝난 도메인은 옛 패키지를 남기지 않는다 (단순 re-export, deprecated alias 모두 금지).
5. **Flyway 변경 없음** — DB 스키마는 그대로. JPA 매핑만 변경.

### 2.2 도메인 간 결합 처리 — 절충안 (Mapper Layer)

현재 4개 엔티티가 `User` 를 ManyToOne 으로 참조한다.

```
Song.registeredBy   → User   (registered_by FK)
Comment.user        → User   (user_id FK)
Playlist.user       → User   (user_id FK)
Like.user           → User   (user_id FK, 복합키 일부)
```

ARCHITECTURE.md 의 도메인 순수성 원칙을 따르되, **infra layer 의 JPA 매핑은 보수적으로 유지**한다.

| 레이어 | User 참조 방식 | 비고 |
|---|---|---|
| `domain/Song` | `Long registeredById` 만 보관 | **Pure POJO** — 다른 도메인 미참조 |
| `infra/persistence/SongEntity` | `@ManyToOne UserEntity registeredBy` 유지 | JPA 매핑 그대로 (cross-domain infra 결합 일시 허용) |
| `infra/persistence/SongEntityMapper` | `entity.registeredBy.id ↔ domain.registeredById` | 변환 책임 |
| 응답에 `username` 필요 시 | `UserQueryRepository.findSummaryByIds(Set<Long>)` 별도 호출 | UseCase 에서 조합 |

**근거:**
- ARCHITECTURE.md "Modular Monolith — Further Work" 항목에 따라 infra 의 cross-domain FK 매핑 제거는 후속 과제
- 한 번에 모든 엔티티의 ManyToOne 을 제거하면 SongQueryRepository (QueryDSL) 와 모든 응답 DTO 를 동시에 손대야 함 → 거대 PR 위험
- 도메인 객체가 다른 도메인을 직접 참조하지 않는다는 핵심 원칙은 100% 달성

**완전 격리(Phase 7+)** : infra 의 ManyToOne 제거는 후속 Phase 에서 별도 진행 (스코프 외)

### 2.3 도메인 마이그레이션 순서

도메인은 독립적으로 마이그레이션한다. 각 Phase 는 별도 worktree + 별도 plan.

| Phase | 도메인 | 이유 |
|---|---|---|
| **7-1** | `user` | `Auth` + `SecurityUtil` 등 공통 인프라가 user 에 의존 — 먼저 안정화. `infra/UserEntity` 가 정해져야 다른 도메인 mapper 가 참조 가능 |
| **7-2** | `character` | 가장 단순 (Read-only 시드). 패턴 검증 |
| **7-3** | `tag` | 단순 (`@PrePersist normalize`) |
| **7-4** | `like` | 격리된 토글. Read/Command 패턴 검증 |
| **7-5** | `comment` | CRUD. `SongEntity` 의존이라 7-7 전 또는 후 가능 |
| **7-6** | `playlist` | CRUD + 다중 엔티티 (Playlist + PlaylistSong) |
| **7-7** | `song` | 가장 복잡 (QueryDSL + Mood + SongCharacter/SongTag) |
| **7-8** | (cleanup) | 옛 패키지 잔재 정리 + 테스트 일괄 재작성 + 빌드 클린 |

각 Phase 후 `main` 으로 `--no-ff` merge.

---

## 3. 패키지 구조 (도메인별)

```
com.vocaloidarchive.<domain>
├── domain/                       # Pure POJO + 도메인 로직
│   ├── <Aggregate>.java          # @Entity 없음, JPA 어노테이션 없음
│   ├── <ValueObject>.java
│   └── <Policy>.java             # 인터페이스 (필요 시)
├── application/                  # UseCase Layer
│   ├── <Verb><Noun>UseCase.java  # 1 UseCase = 1 class
│   ├── <Domain>DomainService.java # 공통 로직 (Facade 부활 금지)
│   ├── port/
│   │   ├── <Domain>Repository.java       # Command port (interface)
│   │   └── <Domain>QueryRepository.java  # Read port (interface, 있으면)
│   └── dto/
│       ├── command/              # UseCase 입력
│       └── result/               # UseCase 출력
├── infra/                        # Infra Layer
│   ├── persistence/
│   │   ├── <Aggregate>Entity.java          # @Entity
│   │   ├── <Aggregate>JpaRepository.java   # extends JpaRepository
│   │   ├── <Aggregate>RepositoryImpl.java  # implements port.Repository
│   │   ├── <Aggregate>QueryRepositoryImpl.java # QueryDSL Read 전용
│   │   └── <Aggregate>EntityMapper.java    # Entity ↔ Domain
│   └── external/                 # 외부 시스템 (있으면)
└── interfaces/                   # Presentation Layer
    ├── <Action>Controller.java   # actor + 액션 단위 분리
    └── dto/
        ├── request/
        └── response/
```

### 3.1 흐름 약속

```
# Command 흐름
interfaces.Controller
  → application.XxxUseCase
  → domain.Xxx (도메인 로직)
  → application.port.XxxRepository (interface)
  → infra.persistence.XxxRepositoryImpl
  → infra.persistence.XxxJpaRepository
  → infra.persistence.XxxEntity ↔ XxxEntityMapper ↔ domain.Xxx

# Read 흐름 (Domain bypass)
interfaces.Controller
  → application.XxxQueryUseCase
  → application.port.XxxQueryRepository
  → infra.persistence.XxxQueryRepositoryImpl (QueryDSL)
  → application.dto.result.XxxResult (Projection)
```

### 3.2 UseCase per-method 분해 규칙

기존 `UserService.signUp / authenticate` → `SignUpUseCase`, `AuthenticateUseCase` 처럼 메서드당 클래스 1개로 분리.
단, **자체 의미가 약한 helper 메서드는 `<Domain>DomainService` 로 모은다** (Facade 회피 위해 도메인 로직만, 인프라 호출 금지).

---

## 4. 공통 인프라 변경

### 4.1 `common/security`

- `CustomUserDetailsService` 는 `user.application.port.UserQueryRepository` 를 의존하도록 변경
- `CustomUserDetails`, `SecurityUtil` 은 변경 없음 (userId 만 사용)

### 4.2 `common/exception`, `common/response`, `common/config`

- 변경 없음. Phase 7 에서 손대지 않는다.

---

## 5. 테스트 전략

### 5.1 마이그레이션 중

- Phase 7-0 ~ 7-7 동안 기존 `*Test` 가 컴파일 깨질 경우 `@Disabled("phase-7-migration")` 처리 허용
- 다만 **각 Phase 종료 시 `./gradlew compileJava compileTestJava` 는 통과해야 한다** (런타임 비활성화는 OK, 컴파일 실패는 불가)

### 5.2 Phase 7-8 (cleanup)

- 모든 `@Disabled("phase-7-migration")` 제거
- UseCase 별 BDD 단위 테스트 작성
- 기존 ServiceTest 는 삭제 (UseCase 기반으로 대체)
- Repository 통합 테스트는 새 `RepositoryImpl` + JpaRepository 기준으로 재작성
- ControllerTest 는 새 controller 구조 기준 재작성

---

## 6. 위험과 대응

| 위험 | 대응 |
|---|---|
| Hibernate 매핑 변경 시 데이터 손상 | `application-test.yml` 의 `ddl-auto: validate` 유지 + Flyway 변경 없음으로 보장 |
| ManyToOne → Long 변경 시 N+1 발생 | Read 흐름은 QueryDSL projection 으로 fetch join 처리 |
| User 도메인 마이그레이션 중 Auth 깨짐 | Phase 7-1 worktree 에서 별도로 진행, 전체 통합 테스트 후 merge |
| 기존 테스트 대량 비활성화 | Phase 7-8 까지 항상 옛 테스트 살아 있는 상태로 진행 (한 Phase 가 다른 도메인 테스트를 깨트리지 못하게 함) |

---

## 7. Out of Scope

- `@QueryDelegate` 도입 (Further Work)
- EDA / Message Broker (Further Work)
- 프런트엔드 변경
- API spec 변경 (응답 포맷 그대로)
- Flyway 마이그레이션 추가

---

## 8. 완료 조건 (DoD)

- [ ] `com.vocaloidarchive.<domain>.{controller,service,repository,dto}` 패키지가 전부 사라졌음
- [ ] 모든 `@Entity` 가 `infra/persistence/` 아래에만 존재
- [ ] 모든 도메인 객체가 다른 도메인 엔티티를 ManyToOne 으로 참조하지 않음
- [ ] `./gradlew clean build` 통과 (테스트 포함)
- [ ] 모든 기존 API 가 동일하게 동작 (응답 포맷 변경 없음)
- [ ] `docs/superpowers/handoff/2026-MM-DD-phase-7-complete.md` 작성

---
