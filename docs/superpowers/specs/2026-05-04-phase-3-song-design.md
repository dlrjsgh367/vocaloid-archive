# Phase 3: Song 도메인 설계 스펙

- 작성일: 2026-05-04
- 상태: 설계 확정 (구현 시작 전)
- 상위 스펙: [`2026-05-03-vocaloid-archive-design.md`](./2026-05-03-vocaloid-archive-design.md) (전체 v1 설계)
- 의존: Phase 1 (프로젝트 부트스트랩), Phase 2 (User + JWT) 완료 상태

---

## 1. 범위

### 1.1 산출물

- `song`, `character`, `tag` 3개 도메인 백엔드 코드 (controller / service / repository / domain / dto)
- `like/domain/Like.java` + `LikeId.java` (엔티티 매핑만 — Phase 4에서 본격 사용)
- `common/util/YoutubeUtil.java` (썸네일 정규식 추출)
- `SongQueryRepository` (QueryDSL 동적 검색 + likes COUNT 서브쿼리 정렬)
- `ErrorCode`에 `SONG_NOT_FOUND`, `CHARACTER_NOT_FOUND` 추가

### 1.2 Phase 3에서 안 하는 것

- Like / Comment / Playlist 서비스/컨트롤러 (Like 엔티티는 정렬용 SELECT 매핑만)
- Character 등록/수정/삭제 API (V2 시드 고정)
- `GET /api/tags` 목록 API
- 프론트엔드 코드
- 트렌딩 정렬, play_count throttle, 이미지 업로드, 캐싱

### 1.3 의존성

- Phase 2 완료된 `SecurityUtil.getCurrentUserId()`, JWT 필터, `User` 엔티티 그대로 활용
- 새 빌드 의존성 추가 없음 (QueryDSL / Validation / Lombok 모두 `build.gradle` 등록됨)
- Flyway 마이그레이션 추가 없음 (V1에 songs/characters/tags/song_characters/song_tags/likes 모두 존재)

### 1.4 상위 스펙 갱신 포인트

상위 스펙 5.2의 `sort=popular`은 Phase 3에서 정상 동작 (likes COUNT 서브쿼리). `SongDetailResponse`에서 `comments` 필드는 제외하고 `GET /api/songs/{id}/comments` 별도 엔드포인트로만 노출 (Phase 4 구현).

---

## 2. 데이터 모델 / JPA 매핑

### 2.1 엔티티 클래스 구성

```
song/domain/
  Song.java               -- @Entity, songs
  SongCharacter.java      -- @Entity, song_characters, @IdClass(SongCharacterId)
  SongCharacterId.java    -- 복합키 (Long songId, Long characterId)
  SongTag.java            -- @Entity, song_tags, @IdClass(SongTagId)
  SongTagId.java          -- 복합키
  Mood.java               -- enum: BRIGHT, DARK, EMOTIONAL, ENERGETIC, CALM
character/domain/
  Character.java          -- @Entity, 테이블 `characters`
tag/domain/
  Tag.java                -- @Entity
like/domain/
  Like.java               -- @Entity, likes, @IdClass(LikeId)
  LikeId.java             -- 복합키 (Long userId, Long songId)
```

`Character`는 클래스명을 그대로 유지 (`java.lang.Character`와 충돌 시 FQN 사용).

### 2.2 매핑 규칙

- `Song.characters`: `@OneToMany(mappedBy="song", cascade=ALL, orphanRemoval=true)` 양방향. 도메인 메서드 `addCharacter(Character)` / `addTag(Tag)`로 노출.
- `Song.tags`: 동일 패턴
- `SongCharacter`, `SongTag`, `Like`: `@ManyToOne(fetch=LAZY)` 양쪽 (song / character 또는 song / tag 또는 song / user)
- `Mood`: `@Enumerated(EnumType.STRING)` (V1 컬럼 VARCHAR(20))
- `Song.playCount`: `Integer`. 증분은 `@Modifying` UPDATE만 사용 (도메인 메서드 없음 → dirty checking 우회)
- `Song.registeredBy`: `@ManyToOne(fetch=LAZY) @JoinColumn(name="registered_by")` User 참조
- `Song.createdAt`: `@CreatedDate` (Phase 1 `AuditingConfig` 활성). Character/Tag/SongCharacter/SongTag/Like는 audit 필드 없음
- `@BatchSize(size=20)`을 `Song.characters`, `Song.tags`에 적용 — 검색 결과 페이지 단위 N+1 회피

### 2.3 DTO

```
song/dto/
  request/
    SongCreateRequest.java   record(title, youtubeUrl, niconicoUrl, bpm, mood, characterIds, tagNames)
    SongSearchRequest.java   record(keyword, mood, characterId, tagId, sort)
    SongSort.java            enum LATEST, POPULAR, PLAYED
  response/
    SongResponse.java        record(id, title, thumbnailUrl, mood, playCount, likeCount, registeredBy:UserSummary, characters:List<CharacterResponse>, tags:List<String>, createdAt) + from(Song, likeCount)
    SongDetailResponse.java  record(... + youtubeUrl, niconicoUrl, bpm) + from(Song, likeCount)
    UserSummary.java         record(id, username)
character/dto/response/
  CharacterResponse.java     record(id, name, colorHex, imageUrl) + from(Character)
```

`SongResponse.likeCount`는 QueryDSL projection의 `count(likes)` 결과로 채움. Phase 3 시점엔 항상 0이지만 필드는 노출.

---

## 3. API 명세 (Phase 3 한정)

전부 base path `/api/**`, `ApiResponse<T>` 래핑.

### 3.1 Song

| 메서드 | 경로 | 인증 | Body / Query | 응답 |
|---|---|---|---|---|
| GET | `/api/songs` | X | `keyword`, `mood`, `characterId`, `tagId`, `sort=latest\|popular\|played` (기본 latest), `page=0`, `size=20` (최대 50) | `PageResponse<SongResponse>` |
| POST | `/api/songs` | O | `SongCreateRequest` | `SongResponse` (201 Created) |
| GET | `/api/songs/{id}` | X | — | `SongDetailResponse`. play_count atomic +1 |
| DELETE | `/api/songs/{id}` | O | — | 204 No Content. 본인(`registered_by`)만, 아니면 403 |

**검색 의미:**
- `keyword`: title contains OR character.name contains OR tag.name contains (대소문자 무시, distinct)
- `mood`: `Mood` enum 정확 일치
- `characterId`, `tagId`: 매핑된 곡만 (exists 서브쿼리)
- `sort=latest`: `createdAt desc, id desc`
- `sort=popular`: `count(likes) desc, createdAt desc, id desc`
- `sort=played`: `playCount desc, createdAt desc, id desc`

`page`는 0-based. `size > 50` → 50 캡, `size < 1` → 1 캡.

### 3.2 Character

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/characters` | X | `List<CharacterResponse>` (id 오름차순) |

페이징 없음 (V2 시드 10명 고정).

### 3.3 Tag

Phase 3엔 공개 엔드포인트 없음. 곡 등록/검색 시 내부 `TagService`만 사용.

### 3.4 SecurityConfig

permitAll 룰은 Phase 2 그대로 (`GET /api/songs/**`, `GET /api/characters` 모두 커버됨). **변경 없음**, 동작 검증만.

### 3.5 ErrorCode 추가

```java
SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "곡을 찾을 수 없습니다")
CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 캐릭터입니다")
```

`sort` 쿼리 파라미터를 enum으로 못 받으면 `MethodArgumentTypeMismatchException` → 기존 `VALIDATION_FAILED` 재사용 (`details: { sort: "..." }`). 새 ErrorCode 추가 안 함.

### 3.6 Validation

**SongCreateRequest:**
- `title`: `@NotBlank @Size(max=200)`
- `youtubeUrl`: `@Pattern(regexp="^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be).*", flags=CASE_INSENSITIVE)` nullable
- `niconicoUrl`: `@Pattern` nullable (`nicovideo\\.jp` / `nico\\.ms`)
- `bpm`: `@Min(40) @Max(300)` nullable
- `mood`: `@NotNull` (Mood enum)
- `characterIds`: `@NotEmpty @Size(max=10)`
- `tagNames`: `@Size(max=10)`, 각 원소 `@NotBlank @Size(min=1, max=30)` (record element validation)

`youtubeUrl`/`niconicoUrl` 동시 null 허용 (v1 정책).

---

## 4. 핵심 흐름

### 4.1 Song 등록 (`SongService.create`)

1. `SecurityUtil.getCurrentUserId()` → `currentUserId`
2. `UserRepository.getReferenceById(currentUserId)` → User proxy
3. `CharacterRepository.findAllById(characterIds)` → 결과 size != 요청 size면 `CHARACTER_NOT_FOUND`
4. `tagNames` 정규화: 각 raw → `trim().toLowerCase()` → 빈 문자열/중복 제거 → distinct list
5. `TagService.findOrCreateAll(normalizedNames)` → `List<Tag>`
   - `findAllByNameIn` → 누락분만 `saveAll(Tag.of(name))`
   - Race 방어: `DataIntegrityViolationException` catch → 재조회
6. `youtubeUrl` 있으면 `YoutubeUtil.extractThumbnailUrl(url)` → 정규식 미스 시 null
7. Song 인스턴스 생성 (registeredBy=user, thumbnailUrl=정규식 결과)
8. `Song.addCharacter(c)` × N, `Song.addTag(t)` × M (양방향 매핑 세팅)
9. `songRepository.save(song)` (cascade로 song_characters/song_tags INSERT)
10. `SongResponse.from(song, likeCount=0)` 반환

트랜잭션: `@Transactional` 메서드 레벨.

### 4.2 Song 검색 (`SongQueryRepository.search`)

QueryDSL `JPAQueryFactory` 주입. 의사코드:

```java
JPQLQuery<Song> query = queryFactory.selectFrom(song).distinct();

BooleanBuilder where = new BooleanBuilder();
if (keyword != null) {
  where.and(song.title.containsIgnoreCase(keyword)
    .or(JPAExpressions.selectOne().from(songCharacter)
        .innerJoin(songCharacter.character, character)
        .where(songCharacter.song.eq(song)
          .and(character.name.containsIgnoreCase(keyword))).exists())
    .or(JPAExpressions.selectOne().from(songTag)
        .innerJoin(songTag.tag, tag)
        .where(songTag.song.eq(song)
          .and(tag.name.containsIgnoreCase(keyword))).exists()));
}
if (mood != null) where.and(song.mood.eq(mood));
if (characterId != null) {
  where.and(JPAExpressions.selectOne().from(songCharacter)
    .where(songCharacter.song.eq(song)
      .and(songCharacter.character.id.eq(characterId))).exists());
}
if (tagId != null) {
  where.and(JPAExpressions.selectOne().from(songTag)
    .where(songTag.song.eq(song)
      .and(songTag.tag.id.eq(tagId))).exists());
}

NumberExpression<Long> likeCountExpr =
  JPAExpressions.select(like.count()).from(like).where(like.song.eq(song));

OrderSpecifier<?>[] order = switch (sort) {
  case POPULAR -> new OrderSpecifier[]{ likeCountExpr.desc(), song.createdAt.desc(), song.id.desc() };
  case PLAYED  -> new OrderSpecifier[]{ song.playCount.desc(), song.createdAt.desc(), song.id.desc() };
  case LATEST  -> new OrderSpecifier[]{ song.createdAt.desc(), song.id.desc() };
};

List<Song> rows = query.where(where).orderBy(order)
  .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();

Long total = queryFactory.select(song.countDistinct())
  .from(song).where(where).fetchOne();

Map<Long, Long> likeCounts = queryFactory
  .select(like.song.id, like.count())
  .from(like).where(like.song.id.in(rows.stream().map(Song::getId).toList()))
  .groupBy(like.song.id)
  .fetch().stream().collect(toMap(t -> t.get(0, Long.class), t -> t.get(1, Long.class)));

return new PageImpl<>(toResponses(rows, likeCounts), pageable, total);
```

### 4.3 Song 상세 (`SongService.findDetail`)

1. `songRepository.incrementPlayCount(id)` (`@Modifying(clearAutomatically=true)` UPDATE). 영향 row 0이면 `SONG_NOT_FOUND`
2. `songRepository.findDetailWithCharacters(id)` (fetch join: registeredBy + characters)
3. `songRepository.findDetailWithTags(id)` (fetch join: tags) — 동일 Song에 tags 컬렉션 채움 (영속성 컨텍스트 동일성 활용)
4. `likeCount`: `JPAQueryFactory`로 `select(like.count()).from(like).where(like.song.id.eq(id))`
5. `SongDetailResponse.from(song, likeCount)`

`@Transactional`. UPDATE → SELECT 같은 트랜잭션. `clearAutomatically=true`로 UPDATE 후 1차 캐시 stale 회피.

### 4.4 Song 삭제 (`SongService.delete`)

1. `song = songRepository.findById(id).orElseThrow(SONG_NOT_FOUND)`
2. `song.getRegisteredBy().getId() != currentUserId` → `BusinessException(FORBIDDEN)`
3. `songRepository.delete(song)` — `song_characters` / `song_tags` / `likes` / `comments` / `playlist_songs` 모두 V1 ON DELETE CASCADE

### 4.5 YoutubeUtil

```java
public final class YoutubeUtil {
  private static final Pattern PATTERN = Pattern.compile(
    "(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/))([A-Za-z0-9_-]{11})",
    Pattern.CASE_INSENSITIVE);

  private YoutubeUtil() {}

  public static String extractThumbnailUrl(String url) {
    if (url == null) return null;
    Matcher m = PATTERN.matcher(url);
    return m.find() ? "https://i.ytimg.com/vi/" + m.group(1) + "/hqdefault.jpg" : null;
  }
}
```

외부 호출 없음. Spring 빈 아님. Service에서 직접 호출.

---

## 5. 패키지 / 파일 매핑

### 5.1 신규 / 수정 파일

| 파일 | Action | 책임 |
|---|---|---|
| `song/domain/Song.java` | Create | JPA 엔티티 + 양방향 매핑 + 도메인 메서드 |
| `song/domain/SongCharacter.java` | Create | @IdClass, @ManyToOne song/character |
| `song/domain/SongCharacterId.java` | Create | 복합키 |
| `song/domain/SongTag.java` | Create | @IdClass, @ManyToOne song/tag |
| `song/domain/SongTagId.java` | Create | 복합키 |
| `song/domain/Mood.java` | Create | enum |
| `song/repository/SongRepository.java` | Create | `JpaRepository`, `incrementPlayCount`, `findDetailWithCharacters`, `findDetailWithTags` |
| `song/repository/SongQueryRepository.java` | Create | QueryDSL 동적 검색 |
| `song/service/SongService.java` | Create | `create`, `search`, `findDetail`, `delete` |
| `song/controller/SongController.java` | Create | `/api/songs` 엔드포인트 4개 |
| `song/dto/request/SongCreateRequest.java` | Create | record + Bean Validation |
| `song/dto/request/SongSearchRequest.java` | Create | record (controller `@ModelAttribute`) |
| `song/dto/request/SongSort.java` | Create | enum |
| `song/dto/response/SongResponse.java` | Create | record + factory |
| `song/dto/response/SongDetailResponse.java` | Create | record + factory |
| `song/dto/response/UserSummary.java` | Create | record(id, username) |
| `character/domain/Character.java` | Create | JPA 엔티티 |
| `character/repository/CharacterRepository.java` | Create | `findAllByOrderByIdAsc` |
| `character/service/CharacterService.java` | Create | `findAll` |
| `character/controller/CharacterController.java` | Create | `GET /api/characters` |
| `character/dto/response/CharacterResponse.java` | Create | record + factory |
| `tag/domain/Tag.java` | Create | JPA 엔티티 |
| `tag/repository/TagRepository.java` | Create | `findAllByNameIn`, `existsByName` |
| `tag/service/TagService.java` | Create | `findOrCreateAll(List<String>)` |
| `like/domain/Like.java` | Create | Phase 3 매핑만 |
| `like/domain/LikeId.java` | Create | 복합키 |
| `common/util/YoutubeUtil.java` | Create | 정규식 추출 (static) |
| `common/exception/ErrorCode.java` | Modify | `SONG_NOT_FOUND`, `CHARACTER_NOT_FOUND` 추가 |
| `common/config/SecurityConfig.java` | Verify | 변경 없음, 검증만 |

### 5.2 테스트 파일

| 파일 | Action | 종류 |
|---|---|---|
| `song/service/SongServiceTest.java` | Create | Mockito BDD (create/search/findDetail/delete) |
| `song/repository/SongRepositoryTest.java` | Create | `@DataJpaTest` + Testcontainers |
| `song/repository/SongQueryRepositoryTest.java` | Create | `@DataJpaTest` + Testcontainers (검색 + 정렬) |
| `song/controller/SongControllerTest.java` | Create | `@WebMvcTest` |
| `character/service/CharacterServiceTest.java` | Create | Mockito |
| `character/controller/CharacterControllerTest.java` | Create | `@WebMvcTest` |
| `tag/service/TagServiceTest.java` | Create | Mockito |
| `tag/repository/TagRepositoryTest.java` | Create | `@DataJpaTest` |
| `common/util/YoutubeUtilTest.java` | Create | 순수 단위 |

---

## 6. 테스트 전략

### 6.1 SongServiceTest (Mockito BDD)

| 시나리오 | 검증 |
|---|---|
| `create` happy | youtubeUrl 정규식 매치 → thumbnail 채워짐, characterIds 검증 통과, tagNames 정규화 후 findOrCreate 호출, song 저장 |
| `create` characterId 누락 | findAllById 결과 size mismatch → `CHARACTER_NOT_FOUND` |
| `create` youtubeUrl 패턴 미스 (`https://example.com/abc`) | thumbnailUrl null로 정상 저장 |
| `create` youtubeUrl null | 정규식 호출 결과 null, thumbnailUrl null |
| `create` tagNames 정규화 | "  Pop  ", "POP", "pop" → 한 번만 findOrCreate("pop") |
| `search` (서비스) | QueryRepository delegate + likeCount 매핑 |
| `findDetail` happy | `incrementPlayCount` 호출 → fetch join → 응답 |
| `findDetail` 없음 | UPDATE 영향 row 0 → `SONG_NOT_FOUND` |
| `delete` 본인 | `repo.delete` 호출 |
| `delete` 타인 | `FORBIDDEN` |
| `delete` 없음 | `SONG_NOT_FOUND` |

### 6.2 SongQueryRepositoryTest (`@DataJpaTest`)

데이터 셋업: User 2명, Character 3종, Tag 3종, Song 4곡 (각각 다른 mood/character/tag 조합), Like 0개

| 시나리오 | 검증 |
|---|---|
| no filter, sort=latest | 4곡 createdAt desc |
| keyword=title 일부 | title 매치만 |
| keyword=character.name 일부 | character 매치 곡만 |
| keyword=tag.name 일부 | tag 매치 곡만 |
| mood=BRIGHT | mood 매치 |
| characterId | 매핑된 곡만 |
| tagId | 매핑된 곡만 |
| mood + characterId 동시 | AND |
| sort=played | playCount desc |
| sort=popular (likes 0) | likeCount 동률 → tiebreaker createdAt desc |
| sort=popular (Like 직접 INSERT) | likeCount desc 정확 |
| pageable size=2 | totalElements=4, totalPages=2 |

### 6.3 SongRepositoryTest

| 시나리오 | 검증 |
|---|---|
| `incrementPlayCount(id)` | 영향 row=1, DB select 후 +1 |
| `incrementPlayCount(없는 id)` | 영향 row=0 |
| `findDetailWithCharacters` | LAZY 초기화 없이 character 컬렉션 접근 |
| `findDetailWithTags` | 동일 |
| Song 삭제 시 song_characters/song_tags CASCADE | 삭제 후 조회 불가 |

### 6.4 SongControllerTest (`@WebMvcTest`)

Phase 2 `AuthControllerTest`처럼 `addFilters=true` 유지, `@MockBean SongService`.

| 시나리오 | 검증 |
|---|---|
| GET `/api/songs` 익명 | 200 + PageResponse |
| POST `/api/songs` 익명 | 401 |
| POST `/api/songs` 인증 + valid | 201 + SongResponse |
| POST validation 실패 (title blank, characterIds 빈, bpm=10) | 400 + VALIDATION_FAILED + details Map |
| POST youtubeUrl 패턴 미스 (`https://example.com`) | 400 |
| GET `/api/songs/{id}` 익명 | 200 |
| GET `/api/songs/{id}` 없음 | 404 |
| DELETE 익명 | 401 |
| DELETE 본인 | 204 |
| DELETE 타인 | 403 |

### 6.5 CharacterServiceTest / ControllerTest

| 시나리오 | 검증 |
|---|---|
| `findAll` | repo 결과 매핑 |
| GET `/api/characters` 익명 | 200 |

### 6.6 TagServiceTest / TagRepositoryTest

| 시나리오 | 검증 |
|---|---|
| `findOrCreateAll(["Pop","pop","  POP  "])` | 정규화 후 1개만 findOrCreate |
| 기존 + 신규 mix | 기존 SELECT, 신규만 INSERT |
| `findAllByNameIn` | 다수 매치 반환 |

Race 시뮬레이션은 v1 생략 (UNIQUE + catch 로직은 service mock으로 검증).

### 6.7 YoutubeUtilTest

| 시나리오 | 검증 |
|---|---|
| `youtu.be/dQw4w9WgXcQ` | thumbnail URL 정확 |
| `https://www.youtube.com/watch?v=dQw4w9WgXcQ` | 동일 |
| `https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s` | 추가 쿼리 무시 |
| `https://www.youtube.com/embed/dQw4w9WgXcQ` | 동일 |
| `https://www.youtube.com/shorts/dQw4w9WgXcQ` | 동일 |
| `https://example.com/dQw4w9WgXcQ` | null |
| `https://www.youtube.com/watch?v=abc` (videoId 11자 미만) | null |
| null 입력 | null |

### 6.8 미커버 / 생략

- E2E `@SpringBootTest` (Phase 2와 동일 정책)
- 동시성 (incrementPlayCount race 다발) — atomic UPDATE는 DB 보장

### 6.9 작업 완료 검증 명령

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.song.*"
cd backend && ./gradlew test --tests "com.vocaloidarchive.character.*"
cd backend && ./gradlew test --tests "com.vocaloidarchive.tag.*"
cd backend && ./gradlew test --tests "com.vocaloidarchive.common.util.*"
cd backend && ./gradlew clean build
```

---

## 7. 정책 / 리스크 / 백로그

### 7.1 트랜잭션 정책

- `SongService`, `CharacterService`, `TagService` 클래스 레벨 `@Transactional(readOnly=true)`
- 쓰기 메서드(`create`, `delete`, `findDetail`, `findOrCreateAll`)는 메서드 레벨 `@Transactional` override
- `findDetail`은 `incrementPlayCount` UPDATE 때문에 쓰기 트랜잭션

### 7.2 N+1 / 페치 정책

- 검색: Song만 페이징, character/tag는 `@BatchSize(size=20)`로 일괄 fetch
- 상세: fetch join 두 번 (characters / tags 분리, distinct로 cartesian 회피)
- 검색 like_count: 단건 GROUP BY 쿼리로 `Map<songId, count>` 채움
- 상세 like_count: 단건 count 쿼리

### 7.3 주요 리스크 / 완화

| 리스크 | 완화 |
|---|---|
| `Character` 클래스명 → `java.lang.Character` import 충돌 | 도메인 코드에선 거의 안 씀. 충돌 시 FQN |
| QueryDSL distinct + 페이징에서 count 쿼리 누락 시 totalElements 틀림 | 별도 count 쿼리 명시적으로 작성 |
| `findOrCreateAll` race | tags.name UNIQUE + DataIntegrityViolation catch 후 재조회 |
| `incrementPlayCount` 후 같은 트랜잭션 SELECT → 1차 캐시 stale | `@Modifying(clearAutomatically=true)` |
| `Like` 엔티티만 만들고 service 없음 → 의도 불분명 | 코드만 두고 활용은 SongQueryRepository 정렬 SELECT에 한정 |
| youtu.be 외 도메인 추가 시 정규식 미커버 | YoutubeUtilTest 패턴 5종 + null/실패 케이스로 회귀 방지 |

### 7.4 Phase 3 종료 후 백로그

`docs/superpowers/backlog.md`에 추가:

- **Phase 4: Like 도메인 구현** — repository / service / controller. 엔티티 매핑은 Phase 3에 박혀있어 신규 마이그레이션 불필요
- **Phase 4: Comment 도메인** — `GET/POST /api/songs/{id}/comments` + `DELETE /api/comments/{id}`. SongDetailResponse는 변경 없음 (별도 엔드포인트 정책 확정)
- **Phase 5+: play_count 정책 재검토** — 현재 GET 1회당 +1, 동일 IP 다발 가능. 트래픽 늘면 Redis throttle 도입
- **Phase 5+: thumbnail 품질 협상** — 현재 `hqdefault.jpg` 고정 (480×360). 디자인 픽 시 `maxresdefault.jpg` 폴백 검토 (모든 영상에 존재 X → 404 fallback chain 필요)
- **Phase 6 디자인 단계: SongResponse 필드 추가 검토** — duration, releaseDate 등 검색/카드 UI 요구에 따라

### 7.5 완료 정의

- 백엔드 컴파일 + `./gradlew clean build` 통과
- 신규 테스트 모두 그린 (서비스 / 리포지토리 / QueryDSL / 컨트롤러 / YoutubeUtil)
- 신규 엔드포인트 4개 + Character GET 1개 수동 smoke (Phase 2 패턴, `httpRequests/` 활용)
- 플랜 파일 모든 task 체크박스 완료
- 기존 테스트 회귀 없음 (전체 `./gradlew test` 그린)

---

## 8. 결정 이력 (Phase 3 한정 브레인스토밍 요약)

- **Q1 — popular 정렬 처리**: 옵션 A (Phase 3 안에서 likes COUNT 서브쿼리로 처리, Phase 4 들어와도 코드 변경 없음)
- **Q2 — SongDetailResponse comments 필드**: 옵션 C (필드 자체 미포함, 별도 엔드포인트만 사용)
- **Q3 — play_count +1 동시성**: 옵션 A (`@Modifying` UPDATE atomic, lost update 회피)
- **Q4 — YouTube 썸네일 추출**: 옵션 A (정규식 + `i.ytimg.com/vi/{id}/hqdefault.jpg`. 외부 호출 없음, 실패 시 null)
- **Like 엔티티 처리**: Phase 3에서 매핑만 생성 (정렬 SELECT용). Service/Controller는 Phase 4
- **`Character` 클래스명**: `java.lang.Character`와 충돌 가능하나 FQN으로 회피, 도메인 일관성 유지
- **`INVALID_SORT` ErrorCode**: 추가하지 않고 기존 `VALIDATION_FAILED` + `details: { sort }` 재사용
