# VocaloidArchive 핸드오프 — Phase 5 완료 (2026-05-06)

## 현재 상태

- **브랜치:** `main`
- **마지막 머지:** `bb00da8 Merge feature/phase-5-playlist: Phase 5 Playlist domain complete`
- **백엔드 테스트:** 171개 전부 통과 (BUILD SUCCESSFUL)
- **프론트엔드:** Phase 1 스캐폴딩만 존재, 실제 UI 미구현

---

## 완료된 Phase 목록

| Phase | 내용 | 머지 커밋 |
|-------|------|-----------|
| 1 | 프로젝트 부트스트랩 (Gradle, Docker, Flyway V1 스키마) | — |
| 2 | User + JWT (회원가입/로그인/refresh/logout) | `fa920da` |
| 3 | Song (등록·QueryDSL 검색·상세·play_count) | `6ff7f0c` |
| 4 | Like (토글) + Comment (생성·삭제·페이징) | `78f8368` |
| 5 | Playlist (CRUD + 곡 추가/삭제, public/private 접근제어) | `bb00da8` |

---

## 백엔드 아키텍처 요약

### 레이어 구조
```
Controller → Service → Repository → Domain (Entity)
Controller → DTO (request/response)
```

### 패키지 (com.vocaloidarchive.*)
```
common/       — config, exception, response, security, util
health/       — GET /api/health
user/         — User + RefreshToken 엔티티, JWT 인증
song/         — Song + SongCharacter + SongTag, QueryDSL 검색
character/    — 읽기 전용 (Flyway V2 시드)
tag/          — 자동 생성 (trim+lowercase 정규화)
like/         — @IdClass 복합키 토글
comment/      — 페이징 댓글
playlist/     — @IdClass 복합키, public/private 접근제어
```

### 핵심 패턴 (반드시 따를 것)

**트랜잭션**
```java
@Service @Transactional(readOnly = true)  // 클래스 레벨
public class XxxService {
  @Transactional  // 쓰기 메서드만 override
  public XxxResponse create(...) { ... }
}
```

**복합키 엔티티** (`Like`, `PlaylistSong` 참고)
```java
@Entity @IdClass(XxxId.class)
public class Xxx {
  @Id @ManyToOne(fetch = FetchType.LAZY) private A a;
  @Id @ManyToOne(fetch = FetchType.LAZY) private B b;
}
public class XxxId implements Serializable {
  private Long a;  // 엔티티 필드명과 동일
  private Long b;
  // 기본 생성자, 2-arg 생성자, equals/hashCode
}
```

**@Modifying 쿼리** — 반드시 `@Transactional` 병행
```java
@Transactional
@Modifying
@Query("DELETE FROM ...")
void deleteBy...(...);
```

**접근 제어** — `SecurityUtil.getCurrentUserId()` 익명 사용자 시 `BusinessException(INVALID_TOKEN)` 던짐
```java
// private 리소스 anonymous 접근 → FORBIDDEN 변환
try {
  currentUserId = securityUtil.getCurrentUserId();
} catch (BusinessException e) {
  throw new BusinessException(ErrorCode.FORBIDDEN);
}
```

**@DataJpaTest 패턴**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class XxxRepositoryTest extends AbstractMysqlContainerTest {
  @PersistenceContext EntityManager em;
  // em.flush(); em.clear(); 쌍으로 사용
}
```

**@WebMvcTest 패턴**
```java
@WebMvcTest(XxxController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
         JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class XxxControllerTest {
  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean XxxService xxxService;
}
```

---

## API 엔드포인트 전체 목록

### Auth
| Method | Path | 인증 | 비고 |
|--------|------|------|------|
| POST | `/api/auth/signup` | X | `{username, email, password}` |
| POST | `/api/auth/login` | X | `{email, password}` → `{accessToken, refreshToken}` |
| POST | `/api/auth/refresh` | X | `{refreshToken}` → 새 토큰 쌍 (rotation) |
| POST | `/api/auth/logout` | O | `{refreshToken}` |

### Song
| Method | Path | 인증 | 비고 |
|--------|------|------|------|
| GET | `/api/songs` | X | 검색·필터링·정렬 (QueryDSL) |
| POST | `/api/songs` | O | youtubeUrl → 썸네일 자동 추출 |
| GET | `/api/songs/{id}` | X | 상세, play_count +1 |
| DELETE | `/api/songs/{id}` | O | 본인만 |

### Character
| Method | Path | 인증 |
|--------|------|------|
| GET | `/api/characters` | X |

### Like
| Method | Path | 인증 | 비고 |
|--------|------|------|------|
| POST | `/api/songs/{id}/like` | O | 토글, `{liked, likeCount}` 반환 |

### Comment
| Method | Path | 인증 | 비고 |
|--------|------|------|------|
| GET | `/api/songs/{id}/comments` | X | 최신순 페이징 |
| POST | `/api/songs/{id}/comments` | O | `{content}` 1-500자 |
| DELETE | `/api/comments/{id}` | O | 본인만 |

### Playlist
| Method | Path | 인증 | 비고 |
|--------|------|------|------|
| GET | `/api/playlists` | O | 내 플레이리스트 목록 |
| POST | `/api/playlists` | O | `{title, isPublic}` |
| GET | `/api/playlists/{id}` | △ | private→본인만, public→누구나 |
| DELETE | `/api/playlists/{id}` | O | 본인만 |
| POST | `/api/playlists/{id}/songs` | O | `{songId}`, 중복 시 skip |
| DELETE | `/api/playlists/{id}/songs/{songId}` | O | 본인만 |

---

## SecurityConfig 현황

```java
.requestMatchers("/api/health").permitAll()
.requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/songs/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/characters").permitAll()
.requestMatchers(HttpMethod.GET, "/api/songs/*/comments").permitAll()
.requestMatchers(HttpMethod.GET, "/api/playlists/*").permitAll()
.anyRequest().authenticated()
```

---

## ErrorCode 전체 목록

```java
VALIDATION_FAILED(400), INVALID_CREDENTIALS(401), INVALID_TOKEN(401),
EXPIRED_TOKEN(401), FORBIDDEN(403), USER_NOT_FOUND(404), SONG_NOT_FOUND(404),
CHARACTER_NOT_FOUND(404), COMMENT_NOT_FOUND(404), PLAYLIST_NOT_FOUND(404),
DUPLICATE_USERNAME(409), DUPLICATE_EMAIL(409), INTERNAL_SERVER_ERROR(500)
```

---

## Flyway 마이그레이션 현황

| 버전 | 내용 |
|------|------|
| V1 | 전체 스키마 (users, songs, characters, song_characters, tags, song_tags, playlists, playlist_songs, likes, comments, refresh_tokens) |
| V2 | 보컬로이드 캐릭터 시드 (미쿠, 린, 렌, 루카, KAITO, MEIKO, GUMI 등) |
| V3 | tags 테이블 collation 변경 |

---

## 다음 Phase (Phase 6 — Frontend v1)

**범위:**
- Vue 3 SPA 실제 UI 구현
- 인증 플로우 (로그인/회원가입, Pinia auth store, JWT 인터셉터)
- 홈 화면 (곡 목록, 검색/필터)
- 곡 상세 (유튜브 임베드, 좋아요, 댓글)
- 곡 등록
- 플레이리스트 관리

**주의사항:**
- Phase 6 진입 시 brainstorming 먼저 (디자인/UX 결정 미확정)
- 프론트 디렉토리: `frontend/src/` (api/, stores/, router/, views/, components/)
- 백엔드 프록시: Vite `vite.config.js`에 `/api → http://localhost:8080` 이미 설정됨

---

## 개발 커맨드

```bash
# 백엔드 테스트
cd backend && ./gradlew test

# 백엔드 실행 (DB docker 필요)
docker compose up db -d
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 개발 서버
cd frontend && npm run dev

# 전체 스택
docker compose up --build
```

## 알려진 이슈

- `application.yml` 기본 `JWT_SECRET` 값이 Base64 디코딩 실패 → 로컬 실행 시 환경변수 주입 필요
- `main` 브랜치가 `origin/main`보다 앞서 있음 (push 안 됨)
