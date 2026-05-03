# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

VocaloidArchive — 보컬로이드 팬 커뮤니티 큐레이션 플랫폼. 사용자가 곡을 등록하고 캐릭터·태그·분위기별로 탐색하며 플레이리스트·좋아요·댓글을 남기는 서비스. Spring Boot 3 백엔드 + Vue 3 프론트엔드 모노레포.

---

## Architecture

```
vocaloid-archive/
├── backend/                    # Spring Boot 3.2, Java 17, Gradle
│   └── src/main/java/com/vocaloidarchive/
│       ├── common/             # 공통 인프라 (config, exception, response, security)
│       ├── health/             # GET /api/health
│       ├── user/               # User + Auth 도메인
│       ├── song/               # Song 도메인 (QueryDSL 검색)
│       ├── character/          # Character 도메인 (시드 고정)
│       ├── tag/                # Tag 도메인 (자동 생성)
│       ├── like/               # Like 도메인
│       ├── comment/            # Comment 도메인
│       └── playlist/           # Playlist 도메인
├── frontend/                   # Vue 3 + Vite + Pinia + Axios
│   └── src/
│       ├── api/                # Axios 인스턴스 + 도메인별 API 함수
│       ├── stores/             # Pinia 스토어 (auth, song)
│       ├── router/             # Vue Router
│       ├── views/              # 페이지 컴포넌트
│       └── components/         # 재사용 컴포넌트
├── docs/superpowers/
│   ├── specs/                  # 설계 스펙 문서
│   ├── plans/                  # 구현 플랜 문서
│   └── backlog.md              # Phase별 백로그
├── docker-compose.yml
└── .env.example
```

### Layer Dependencies

```
Controller → Service → Repository → Domain (Entity)
Controller → DTO (request/response)
Service → common/security (SecurityUtil, JwtTokenProvider)
```

### Package Convention

각 도메인은 `controller / service / repository / domain / dto/request / dto/response` 구조를 따른다.  
공통 인프라는 `common/config`, `common/exception`, `common/response`, `common/security`, `common/util`에 위치.

---

## Development Commands

### Build and Run

```bash
# 백엔드 빌드
cd backend && ./gradlew clean build

# 백엔드 로컬 실행 (DB만 docker로 띄운 상태)
docker compose up db -d
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 개발 서버
cd frontend && npm run dev

# 전체 스택 (docker compose)
docker compose up --build
```

### Testing

```bash
# 백엔드 전체 테스트
cd backend && ./gradlew test

# 특정 테스트 클래스
cd backend && ./gradlew test --tests "com.vocaloidarchive.user.service.UserServiceTest"

# 프론트엔드 (v1 생략, Phase 6 이후)
```

### Code Generation

```bash
# QueryDSL Q-class 생성
cd backend && ./gradlew compileJava

# Flyway 마이그레이션 (앱 시작 시 자동 실행)
# 수동 확인: docker exec -it <db-container> mysql -u root -p vocaloid_archive
```

---

## Environment Profiles

| Profile | 설명 |
|---------|------|
| `local` | IDE 직접 실행. `application-local.yml` 활성. DB는 `docker compose up db`로 별도 기동 |
| `docker` | `docker compose up` 전체 스택. `application-docker.yml` 활성. 환경변수 주입 |
| `test` | `@ActiveProfiles("test")`. Testcontainers MySQL 자동 기동. `AbstractMysqlContainerTest` 상속 필요 |

---

## Key Technologies

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Security 6, JPA (Hibernate 6), QueryDSL 5.0.0:jakarta, Flyway
- **Database**: MySQL 8.0
- **Auth**: JJWT 0.12.5 (HS256), BCrypt (strength 10), Refresh Token DB 저장 (SHA-256 해시)
- **Frontend**: Vue 3.4+, Vite 5+, Pinia 2+, Vue Router 4+, Axios 1+
- **Testing**: JUnit 5, Mockito (BDD style), Testcontainers MySQL 1.19.7, `@WebMvcTest`, `@DataJpaTest`
- **Build**: Gradle Groovy DSL

---

## Domain Contexts

| 도메인 | 설명 |
|--------|------|
| User/Auth | 회원가입/로그인/refresh/logout. JWT Access(30분) + Refresh(14일) |
| Song | 곡 등록·검색(QueryDSL)·상세. youtubeUrl → 썸네일 자동 추출 |
| Character | 보컬로이드 캐릭터 목록 (Flyway V2 시드 고정, 읽기 전용) |
| Tag | 곡 등록 시 자동 생성. `trim().toLowerCase()` 정규화 |
| Like | 단일 토글 엔드포인트. 응답에 `liked`, `likeCount` 포함 |
| Comment | 생성·삭제(본인만). 수정 없음. 최신순 페이징 |
| Playlist | CRUD + 곡 추가/삭제. public/private 권한 |

---

## API Conventions

- Base path: `/api/**`
- 모든 응답은 `ApiResponse<T>` 래퍼 사용:
  ```json
  // 성공
  { "success": true, "data": {...}, "message": null, "error": null }
  // 실패
  { "success": false, "data": null, "message": null,
    "error": { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다", "details": null } }
  ```
- Validation 실패 시 `error.details`는 `Map<String, String>` (필드명 → 메시지)
- 페이지 응답: `PageResponse<T> { content, page, size, totalElements, totalPages }`
- 인증: `Authorization: Bearer <accessToken>` 헤더
- 페이지 기본 사이즈 20, 최대 50

---

## Code Quality Standards

- **Java**: Google Java Style Guide (2-space indent, K&R braces)
- **트랜잭션**: 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional` override
- **예외**: `BusinessException(ErrorCode)` 중심. `GlobalExceptionHandler`에서 일괄 처리
- **테스트**: BDD 스타일 (`given/when/then` 구조, `BDDMockito`)
- **DTO**: Java record 우선. Entity 직접 노출 금지

---

## Worktree Configuration

프로젝트 로컬 워크트리 디렉토리: `.worktrees/`

```bash
# 워크트리 생성 예시
git worktree add .worktrees/phase-2-user-jwt -b feature/phase-2-user-jwt
```

`.worktrees/`는 `.gitignore`에 등록되어 있어야 한다.

---

## Commit Message Convention

Conventional Commits 형식 사용 (이모지 없음):

```
type(scope): subject

# 예시
feat(user): add UserService signup and authenticate
test(user): add UserService BDD unit tests
fix(security): handle ExpiredJwtException in filter
docs: add Phase 2 design spec
build(backend): upgrade Spring Boot to 3.2.5
```

### Type 목록

| Type | 사용 상황 |
|------|-----------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `test` | 테스트 코드 추가·수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서 추가·수정 |
| `build` | 빌드 설정, 의존성 변경 |
| `ci` | CI/CD 설정 변경 |
| `chore` | 기타 (설정 파일, .gitignore 등) |
| `style` | 포맷·공백 등 코드 스타일만 변경 |

### 규칙

- `subject`는 소문자로 시작, 마침표 없음
- `scope`는 도메인명 또는 레이어명 (예: `user`, `security`, `frontend`)
- 본문이 필요한 경우 빈 줄 후 작성

---

## Branch Strategy

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포 가능한 상태 유지. Phase 완료 후 merge |
| `feature/phase-N-*` | 각 Phase 구현 브랜치 (예: `feature/phase-2-user-jwt`) |
| `fix/*` | 버그 수정 브랜치 |

### Merge 규칙

- `feature/*` → `main`: Phase 완료 후 merge commit (`git merge --no-ff`)
- PR/MR 불필요 (개인 프로젝트), 단 Phase 완료 후 `docs/superpowers/plans/` 플랜 체크 완료 확인
