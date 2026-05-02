# VocaloidArchive 설계 스펙

- 작성일: 2026-05-03
- 상태: v1 설계 확정 (구현 시작 전)
- 범위: 핸드오프 문서 8개 도메인 전체를 v1으로 묶어 단일 사이클 구현

---

## 1. 프로젝트 개요

보컬로이드 팬 커뮤니티 웹 서비스. 사용자가 보컬로이드 곡을 직접 등록하고, 캐릭터·태그·분위기별로 탐색하고, 플레이리스트를 만들고 좋아요·댓글을 남기는 큐레이션 플랫폼.

### 1.1 v1 범위 (단일 사이클)

User · Auth · Song · Character · Tag · Like · Comment · Playlist 8개 도메인 전체.

### 1.2 v2 이후로 미루는 항목

- 프로필 이미지/캐릭터 이미지 업로드 (v1은 외부 URL 입력만)
- Playlist 곡 재정렬 (v1은 추가 시 `order_index = max+1` 고정)
- Playlist 메타 수정 (제목, 공개 여부 변경)
- Comment 수정 (v1은 삭제만)
- 트렌딩 정렬 (최근 7일 가중치)
- 어드민 권한 / 캐릭터 관리 UI
- 이메일 인증, 비밀번호 재설정
- play_count throttle / dedup
- 프론트엔드 자동화 테스트
- 운영용 Docker multi-stage build, CI 파이프라인

---

## 2. 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Spring Boot 3.2+, Java 17, Spring Security, JPA, QueryDSL, MySQL 8.0, Flyway |
| Frontend | Vue 3 (Vite), Pinia, Vue Router, Axios |
| Infra | Docker Compose (로컬), 추후 OCI Free Tier |
| 인증 | 자체 회원가입/로그인, JWT (Access 30분 + Refresh 14일, Refresh DB 저장) |
| Build | Gradle Groovy DSL |
| 테스트 | JUnit 5, Mockito (BDD), `@DataJpaTest`, `@WebMvcTest`, Testcontainers MySQL |
| 레포 | 모노레포 단일 Git 저장소 |

---

## 3. 시스템 아키텍처

```
┌─────────────────┐    HTTPS/JSON    ┌─────────────────────┐
│  Vue 3 SPA      │ ←──────────────→ │  Spring Boot 3 API   │
│  (Vite, Pinia)  │   JWT in Header  │  (Java 17, JPA, QDSL)│
│  port 5173      │                  │  port 8080           │
└─────────────────┘                  └──────────┬──────────┘
                                                │ JDBC
                                                ↓
                                     ┌─────────────────────┐
                                     │    MySQL 8.0        │
                                     │    port 3306        │
                                     │    Flyway migration │
                                     └─────────────────────┘

배포 단위: docker-compose 3개 컨테이너 (frontend, backend, db)
```

### 3.1 디렉토리 구조 (루트)

```
vocaloid-archive/
├── backend/
├── frontend/
├── docs/superpowers/specs/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## 4. 데이터 모델

### 4.1 테이블 정의

#### users

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK, auto increment |
| username | VARCHAR(50) | unique |
| email | VARCHAR(100) | unique |
| password_hash | VARCHAR(255) | BCrypt |
| profile_image_url | VARCHAR(500) | nullable, 외부 URL |
| created_at | TIMESTAMP | |

#### refresh_tokens

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users.id, 인덱스 |
| token_hash | VARCHAR(255) | SHA-256 해시 (raw token 저장 X) |
| expires_at | TIMESTAMP | |
| created_at | TIMESTAMP | |

#### songs

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| registered_by | BIGINT | FK → users.id |
| title | VARCHAR(200) | |
| youtube_url | VARCHAR(500) | nullable |
| niconico_url | VARCHAR(500) | nullable |
| thumbnail_url | VARCHAR(500) | nullable, youtube_url에서 자동 추출 |
| bpm | INT | nullable |
| mood | ENUM | BRIGHT, DARK, EMOTIONAL, ENERGETIC, CALM |
| play_count | INT | default 0 |
| created_at | TIMESTAMP | |

#### characters

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(100) | unique, 시드로 고정 |
| color_hex | VARCHAR(7) | 시그니처 컬러 |
| image_url | VARCHAR(500) | nullable |

#### song_characters (다대다 중간)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| song_id | BIGINT | FK → songs.id |
| character_id | BIGINT | FK → characters.id |

PK: (song_id, character_id). 인덱스: character_id

#### tags

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR(50) | unique, trim+lowercase 정규화 후 저장 |

#### song_tags (다대다 중간)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| song_id | BIGINT | FK → songs.id |
| tag_id | BIGINT | FK → tags.id |

PK: (song_id, tag_id). 인덱스: tag_id

#### playlists

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users.id |
| title | VARCHAR(200) | |
| is_public | BOOLEAN | default true |
| created_at | TIMESTAMP | |

#### playlist_songs

| 컬럼 | 타입 | 비고 |
|---|---|---|
| playlist_id | BIGINT | FK → playlists.id |
| song_id | BIGINT | FK → songs.id |
| order_index | INT | 추가 시 max+1 |

PK: (playlist_id, song_id). 인덱스: (playlist_id, order_index)

#### likes

| 컬럼 | 타입 | 비고 |
|---|---|---|
| user_id | BIGINT | FK → users.id |
| song_id | BIGINT | FK → songs.id |
| liked_at | TIMESTAMP | |

PK: (user_id, song_id). 인덱스: song_id

#### comments

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT | PK |
| user_id | BIGINT | FK → users.id |
| song_id | BIGINT | FK → songs.id |
| content | VARCHAR(500) | 1-500자 |
| created_at | TIMESTAMP | |

인덱스: (song_id, created_at)

### 4.2 인덱스 전략

- `songs.created_at` (목록 정렬)
- `songs.play_count` (인기순 정렬)
- `song_characters.character_id` (캐릭터 필터)
- `song_tags.tag_id` (태그 필터)
- `comments(song_id, created_at)` (댓글 페이징)
- `likes.song_id` (좋아요 수 카운트)
- `playlist_songs(playlist_id, order_index)` (재생 순서)

### 4.3 JPA 매핑 결정

- **다대다 중간 테이블**: `@ManyToMany` 대신 `SongCharacter` / `SongTag` / `PlaylistSong`을 별도 `@Entity`로 모델링. `@IdClass`로 복합키.
- **Like**: `Like` 엔티티, `@IdClass`로 (user_id, song_id) 복합키.
- **Mood ENUM**: `@Enumerated(EnumType.STRING)`.
- **Audit**: `@CreatedDate`만 사용 (updated_at 없음). `@EnableJpaAuditing` 활성화.

### 4.4 Flyway 마이그레이션

- `V1__init_schema.sql`: 전체 테이블 + 인덱스
- `V2__seed_characters.sql`: 보컬로이드 메이저 캐릭터 시드 (10명 내외)
  - 하츠네 미쿠 #39C5BB, 카가미네 린 #FFE211, 카가미네 렌 #FFC56C, 메구리네 루카 #FFC0CB, KAITO #1E90FF, MEIKO #E0233F, GUMI #94C947, IA, 카후, etc.

---

## 5. API 명세

### 5.1 응답 래퍼 (모든 엔드포인트 공통)

```json
// 성공
{ "success": true, "data": {...}, "message": null, "error": null }
// 실패
{ "success": false, "data": null, "message": null, "error": { "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다" } }
```

페이지 응답은 `PageResponse<T> { content, page, size, totalElements, totalPages }`로 래핑.

### 5.2 엔드포인트 목록

#### Auth

| 메서드 | 경로 | 인증 | 요청 | 응답 |
|---|---|---|---|---|
| POST | `/api/auth/signup` | X | `{username, email, password}` | `UserResponse` |
| POST | `/api/auth/login` | X | `{email, password}` | `TokenResponse {accessToken, refreshToken}` |
| POST | `/api/auth/refresh` | X | `{refreshToken}` | `TokenResponse` (회전: 새 refresh 발급, 구 token 무효화) |
| POST | `/api/auth/logout` | O | `{refreshToken}` | `void` (DB에서 refresh 삭제) |

#### Song

| 메서드 | 경로 | 인증 | 비고 |
|---|---|---|---|
| GET | `/api/songs?keyword=&mood=&characterId=&tagId=&sort=latest\|popular\|played&page=0&size=20` | X | `Page<SongResponse>`. size 기본 20 / 최대 50 |
| POST | `/api/songs` | O | `SongCreateRequest`. youtubeUrl에서 thumbnail 자동 추출 |
| GET | `/api/songs/{id}` | X | `SongDetailResponse` (캐릭터/태그/댓글 포함). play_count +1 |
| DELETE | `/api/songs/{id}` | O | 본인만 (registered_by) |

검색 키워드 범위: 제목 + 캐릭터명 + 태그명 (distinct 처리).
정렬: latest=`createdAt.desc`, popular=`likeCount.desc`, played=`playCount.desc`.

#### Character

| 메서드 | 경로 | 인증 |
|---|---|---|
| GET | `/api/characters` | X |

#### Like

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| POST | `/api/songs/{id}/like` | O | `{liked: boolean, likeCount: int}` (토글) |

#### Comment

| 메서드 | 경로 | 인증 | 비고 |
|---|---|---|---|
| GET | `/api/songs/{id}/comments?page=0&size=20` | X | 최신순 |
| POST | `/api/songs/{id}/comments` | O | `{content}` 1-500자 |
| DELETE | `/api/comments/{id}` | O | 본인만 |

#### Playlist

| 메서드 | 경로 | 인증 | 비고 |
|---|---|---|---|
| GET | `/api/playlists` | O | 내 플레이리스트만 |
| POST | `/api/playlists` | O | `{title, isPublic}` |
| GET | `/api/playlists/{id}` | △ | private이면 본인만, public이면 누구나 |
| DELETE | `/api/playlists/{id}` | O | 본인만 |
| POST | `/api/playlists/{id}/songs` | O | `{songId}`. order_index = max+1 |
| DELETE | `/api/playlists/{id}/songs/{songId}` | O | 본인만 |

### 5.3 ErrorCode 초기 정의

```
USER_NOT_FOUND(404)
DUPLICATE_USERNAME(409)
DUPLICATE_EMAIL(409)
INVALID_CREDENTIALS(401)
INVALID_TOKEN(401)
EXPIRED_TOKEN(401)
SONG_NOT_FOUND(404)
CHARACTER_NOT_FOUND(404)
COMMENT_NOT_FOUND(404)
PLAYLIST_NOT_FOUND(404)
FORBIDDEN(403)
VALIDATION_FAILED(400)
```

---

## 6. 백엔드 패키지 구조

```
src/main/java/com/vocaloidarchive/
├── VocaloidArchiveApplication.java
├── common/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JpaConfig.java
│   │   └── WebConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── response/
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── CustomUserDetails.java
│   └── util/
│       ├── SecurityUtil.java
│       └── YoutubeUtil.java
├── user/
│   ├── controller/UserController.java
│   ├── service/{UserService, RefreshTokenService}.java
│   ├── repository/{UserRepository, RefreshTokenRepository}.java
│   ├── domain/{User, RefreshToken}.java
│   └── dto/
│       ├── request/{SignUpRequest, LoginRequest, RefreshRequest}.java
│       └── response/{UserResponse, TokenResponse}.java
├── song/
│   ├── controller/SongController.java
│   ├── service/SongService.java
│   ├── repository/{SongRepository, SongQueryRepository}.java
│   ├── domain/{Song, SongCharacter, SongTag, Mood}.java
│   └── dto/
│       ├── request/{SongCreateRequest, SongSearchRequest}.java
│       └── response/{SongResponse, SongDetailResponse}.java
├── character/
│   ├── controller/CharacterController.java
│   ├── service/CharacterService.java
│   ├── repository/CharacterRepository.java
│   ├── domain/Character.java
│   └── dto/response/CharacterResponse.java
├── tag/
│   ├── service/TagService.java
│   ├── repository/TagRepository.java
│   └── domain/Tag.java
├── like/
│   ├── controller/LikeController.java
│   ├── service/LikeService.java
│   ├── repository/LikeRepository.java
│   └── domain/Like.java
├── comment/
│   ├── controller/CommentController.java
│   ├── service/CommentService.java
│   ├── repository/CommentRepository.java
│   ├── domain/Comment.java
│   └── dto/
│       ├── request/CommentCreateRequest.java
│       └── response/CommentResponse.java
└── playlist/
    ├── controller/PlaylistController.java
    ├── service/PlaylistService.java
    ├── repository/PlaylistRepository.java
    ├── domain/{Playlist, PlaylistSong}.java
    └── dto/
        ├── request/{PlaylistCreateRequest, PlaylistSongAddRequest}.java
        └── response/{PlaylistResponse, PlaylistDetailResponse}.java

src/main/resources/
├── application.yml
├── application-local.yml
├── application-docker.yml
└── db/migration/
    ├── V1__init_schema.sql
    └── V2__seed_characters.sql
```

### 6.1 핵심 흐름

**Song 등록 (`SongService.create`)**

1. 인증 사용자 확인 (`SecurityUtil.currentUserId()`)
2. youtubeUrl → `YoutubeUtil.extractThumbnailUrl()`
3. characterIds 검증 (존재 여부)
4. tagNames → `TagService.findOrCreateAll(normalize)`
5. Song 저장 → SongCharacter / SongTag 매핑 저장
6. SongResponse 반환

**Song 검색 (`SongQueryRepository.search`)**

- BooleanBuilder 동적 조건:
  - keyword: title.contains OR character.name.contains OR tag.name.contains (distinct)
  - mood, characterId, tagId 각각 join 조건
- 정렬: latest / popular(likeCount) / played(playCount)
- Pageable 페이징

**JWT 인증 흐름**

- `JwtAuthenticationFilter` (OncePerRequestFilter): Authorization Bearer 추출 → `JwtTokenProvider.validate` → SecurityContextHolder 채움
- `SecurityConfig`: `/api/auth/**`, GET `/api/songs/**`, GET `/api/characters`, GET `/api/songs/*/comments`는 permitAll, 그 외 authenticated

---

## 7. 프론트엔드 구조

### 7.1 디렉토리

```
frontend/
├── index.html
├── vite.config.js                    # proxy: /api → http://localhost:8080
├── package.json
└── src/
    ├── main.js
    ├── App.vue
    ├── assets/styles/global.css
    ├── api/
    │   ├── index.js                  # axios + JWT 인터셉터
    │   ├── user.js
    │   ├── song.js
    │   ├── character.js
    │   ├── comment.js
    │   └── playlist.js
    ├── router/index.js
    ├── stores/
    │   ├── auth.js
    │   └── song.js
    ├── views/
    │   ├── HomeView.vue
    │   ├── SongDetailView.vue
    │   ├── SongCreateView.vue
    │   ├── SearchView.vue
    │   ├── PlaylistView.vue
    │   └── auth/
    │       ├── LoginView.vue
    │       └── SignUpView.vue
    └── components/
        ├── common/{AppHeader, AppFooter, BaseButton}.vue
        ├── song/{SongCard, SongList, SongFilter, YoutubeEmbed}.vue
        ├── playlist/PlaylistCard.vue
        └── comment/{CommentList, CommentForm}.vue
```

### 7.2 라우트

```js
[
  { path: '/',          component: HomeView },
  { path: '/songs/:id', component: SongDetailView },
  { path: '/songs/new', component: SongCreateView, meta: { requiresAuth: true } },
  { path: '/search',    component: SearchView },
  { path: '/playlists', component: PlaylistView,   meta: { requiresAuth: true } },
  { path: '/login',     component: LoginView },
  { path: '/signup',    component: SignUpView },
]
```

가드: `meta.requiresAuth=true` 페이지는 비로그인 시 `/login?return=...`로 리다이렉트.

### 7.3 인증 / 토큰 처리

- **Pinia auth store**: state(user, accessToken, refreshToken), actions(login/logout/refresh/fetchMe)
- **accessToken은 메모리만**: XSS 노출 최소화
- **refreshToken만 localStorage**: 앱 재시작 시 자동 로그인
- **Axios 인터셉터**:
  - 요청 전: accessToken 있으면 Authorization 헤더 주입
  - 응답 401 (`EXPIRED_TOKEN`): refresh 시도 → 원 요청 재시도 → 실패 시 logout + `/login`
  - 동시 401 다발 시 refresh in-flight Promise 공유

### 7.4 UI/디자인 디테일

색감, 레이아웃, 타이포, 컴포넌트별 마크업/스타일은 **Phase 6 진입 시점에 별도 brainstorming**으로 결정. 이 스펙은 컴포넌트 책임과 props 흐름까지만 명시.

---

## 8. 인프라 / 환경

### 8.1 docker-compose.yml

```yaml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: vocaloid_archive
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
    ports: ["3306:3306"]
    volumes: [db_data:/var/lib/mysql]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      retries: 10

  backend:
    build: ./backend
    ports: ["8080:8080"]
    depends_on:
      db: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/vocaloid_archive
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
      JWT_SECRET: ${JWT_SECRET}
      APP_CORS_ORIGINS: http://localhost:5173

  frontend:
    build: ./frontend
    ports: ["5173:5173"]
    depends_on: [backend]
    environment:
      VITE_API_BASE_URL: http://localhost:8080/api

volumes:
  db_data:
```

### 8.2 환경 분리

- **로컬 (IDE 실행)**: `application-local.yml` + localhost 접근, MySQL은 docker-compose의 db만 띄움
- **Docker compose 전체**: `application-docker.yml` 활성화
- **운영 (OCI Free Tier)**: 추후, 환경변수로만 주입

### 8.3 시크릿 관리

- `.env`는 `.gitignore` (MYSQL_ROOT_PASSWORD, JWT_SECRET)
- `.env.example` 커밋 (값 X, 키만)
- JWT 시크릿: 256bit 이상 랜덤 (HS256)

### 8.4 빌드/실행

- Backend: `./gradlew clean build` → Dockerfile에서 jar 복사 → `java -jar`
- Frontend: dev는 `npm run dev` (5173 hot reload). 운영용 multi-stage build는 OCI 배포 시점에 추가.

---

## 9. 테스트 전략

| 계층 | 테스트 종류 | 도구 | 범위 |
|---|---|---|---|
| Service | 단위 | JUnit 5 + Mockito (BDD style) | 비즈니스 로직, Repository 모두 mock |
| Repository | 통합 | `@DataJpaTest` + Testcontainers MySQL | 쿼리 정확성, QueryDSL 동적 조건 |
| Controller | slice | `@WebMvcTest` + `@MockBean` + Spring Security MockMvc | 요청/응답, 인증, validation |
| End-to-end | 선택 | `@SpringBootTest` | v1은 인증 + Song 등록/조회 핵심 시나리오만 |

**커버리지 목표**: Service 핵심 메서드 80%+, Repository QueryDSL 쿼리 100%, Controller happy path + 주요 에러.

**프론트엔드 테스트**: v1 생략.

---

## 10. 에러 처리 / 검증 / 보안

### 10.1 에러 처리 표준

`BusinessException` + `ErrorCode` ENUM 중심. `GlobalExceptionHandler`에서:

| 예외 | 응답 |
|---|---|
| `BusinessException` | errorCode.httpStatus + ApiResponse.error |
| `MethodArgumentNotValidException` | 400 + VALIDATION_FAILED + 필드 details |
| `AccessDeniedException` | 403 + FORBIDDEN |
| `AuthenticationException` | 401 + INVALID_TOKEN |
| `Exception` (fallback) | 500 + 일반 메시지 (스택트레이스 로그) |

### 10.2 입력 검증 (Bean Validation)

**SignUpRequest**
- username: NotBlank, 3-20자, `^[a-zA-Z0-9_]+$`
- email: NotBlank, Email, ≤100자
- password: NotBlank, 8-72자, 영문 + 숫자 혼합

**SongCreateRequest**
- title: NotBlank, ≤200자
- youtubeUrl: youtube.com 또는 youtu.be 패턴
- bpm: 40-300 (nullable)
- mood: NotNull (Mood enum)
- characterIds: NotEmpty, ≤10개
- tagNames: ≤10개, 각 1-30자

**CommentCreateRequest**
- content: NotBlank, 1-500자

### 10.3 비즈니스 검증 (Service)

중복 username/email, 캐릭터 존재 여부, 권한 확인. 모두 `BusinessException`으로 표준화.

### 10.4 보안

- 비밀번호: BCrypt (strength 10)
- JWT: HS256, 시크릿 환경변수, refresh token DB에 SHA-256 해시 저장
- SQL Injection: JPA + QueryDSL 파라미터 바인딩
- XSS: Vue 자동 이스케이프, `v-html` 금지
- CORS: WebConfig allowlist (localhost:5173 + ${APP_CORS_ORIGINS})

### 10.5 트랜잭션 정책

- 모든 Service 메서드에 `@Transactional` (쓰기) / `@Transactional(readOnly=true)` (읽기)
- 클래스 레벨 readOnly 기본, 쓰기 메서드 단위 override

---

## 11. 정책 디테일

| 항목 | 정책 |
|---|---|
| 태그 정규화 | `trim().toLowerCase()`, 공백 제거. unique 제약 |
| 태그 최대 | 곡당 ≤10개, 태그명 1-30자 |
| Like | 단일 토글 엔드포인트, 응답에 `liked, likeCount` |
| Comment | 수정 불가, 본인 삭제만. 평문 1-500자 |
| Comment 정렬 | 최신순 |
| play_count | GET /api/songs/{id} 시 +1, throttle 없음 |
| Playlist 권한 | public이면 GET 비로그인 가능, 그 외 본인만 |
| Playlist 곡 순서 | order_index INT, 추가 시 max+1. 재정렬 v2 |
| 회원가입 검증 | username 3-20자(영숫자+_), email 표준, password 8자+ 영숫자 |
| 이메일 인증 | 없음 (v1) |
| 소프트 삭제 | 없음, hard delete |
| Build | Gradle Groovy DSL |
| 페이지 사이즈 | 기본 20 / 최대 50 |
| 정렬 옵션 | latest / popular / played |
| Flyway | V1 schema, V2 character seed |

---

## 12. 구현 우선순위 (Phase 분할)

각 phase는 별도 plan + branch + 머지 사이클로 진행.

1. **Phase 1: 프로젝트 초기 세팅** — backend Spring Boot + build.gradle + application.yml + 공통 클래스(ApiResponse, ErrorCode, GlobalExceptionHandler, SecurityConfig 골격), frontend Vite+Vue3 + router + pinia + axios, docker-compose.yml, Flyway V1+V2
2. **Phase 2: User 도메인** — 회원가입/로그인/refresh/logout API, JWT 발급 및 필터, 인증 가드 검증
3. **Phase 3: Song 도메인** — Song/Character/Tag 엔티티, 등록/목록(필터링)/상세 조회 API, SongQueryRepository (QueryDSL), Character 목록 API, Tag 자동 생성
4. **Phase 4: Like / Comment** — 좋아요 토글, 댓글 CRUD
5. **Phase 5: Playlist** — Playlist + PlaylistSong CRUD, 공개/비공개 권한
6. **Phase 6: Frontend Views** — 진입 시점에 UI/디자인 별도 brainstorming 후, 각 뷰 구현 (Home/Detail/Create/Search/Playlist/Auth)

---

## 13. 결정 이력 (브레인스토밍 요약)

- **MVP 범위**: 옵션 A (핸드오프 8개 도메인 전체를 v1으로)
- **이미지/썸네일**: 옵션 A (URL만, 업로드 없음. youtube 썸네일 자동 추출)
- **캐릭터 데이터**: 옵션 A (Flyway 시드로 고정, 어드민 권한 없음)
- **JWT 정책**: 옵션 A (Access 30분 / Refresh 14일, refresh_tokens DB 저장)
- **검색/정렬/페이징**: 검색 B (제목+캐릭터+태그) / 정렬 B (latest/popular/played) / 페이지 A (offset 기반 Pageable, 기본 20 / 최대 50)
- **Vue UI 디자인**: Phase 6 진입 시 별도 brainstorming
