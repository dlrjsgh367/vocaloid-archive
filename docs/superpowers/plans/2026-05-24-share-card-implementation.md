# 플레이리스트 공유 카드 구현 플랜

> **For agentic workers:** 이 플랜은 태스크 단위로 구현한다. 각 스텝은 체크박스(`- [ ]`)로 추적. TDD(테스트 먼저 → 실패 확인 → 구현 → 그린)를 따르고, 태스크 끝마다 커밋한다.

**Goal:** 공개 플레이리스트를 데코덴 이미지 카드로 export하고, 동적 OG로 SNS 미리보기 + 다운로드를 지원해 유입 루프를 만든다.

**스펙:** [`docs/superpowers/specs/2026-05-24-share-card-design.md`](../specs/2026-05-24-share-card-design.md) (흐름도: `2026-05-24-share-card-flow.html`)

**Tech Stack:** Spring Boot 3.2 (clean arch: interfaces/application/domain/infra), MySQL 8 + Flyway, Node 20 + Playwright(크로미움), nginx, Vue 3 + Pinia, docker-compose.prod.

**확정 결정 (스펙 §10.1):** 렌더=헤드리스 크로미움 별도 컨테이너 · OG=nginx 크롤러 UA 감지 → Spring SSR · 단축코드=`share_code` lazy 발급 · footer ♥=곡 좋아요 합 · 대표 캐릭터=최빈+id tiebreak(없으면 미쿠 `#39C5BB`) · 카드=1080×1080 정사각.

---

## Context (read before coding)

**현재 코드 전제 (확인 완료):**
- `playlists`: `id, user_id, title, is_public, created_at`. 다음 Flyway = **V4**.
- `GetPlaylistDetailUseCase`가 이미 public/private 강제 (public 익명 OK).
- `PlaylistQueryRepositoryImpl`은 `jpa`(PlaylistJpaRepository) + `songJpa`(PlaylistSongJpaRepository)로 조회. 곡 썸네일은 `YoutubeUtil.resolveThumbnailUrl(stored, youtube)`로 유도.
- `Mood` enum: `BRIGHT, DARK, EMOTIONAL, ENERGETIC, CALM`.
- Character 시드에 `color_hex` 존재. 곡↔캐릭터 = `song_characters`, 좋아요 = `likes(user_id, song_id)`.
- 마이그레이션 이전 playlist 테스트는 전부 `@Disabled` 스텁 → 새 테스트는 클린아키텍처 레이어 기준으로 신규 작성.

**레이어 규칙:** Controller(interfaces) → UseCase(application) → Port(application/port) ← Impl(infra). DTO는 record. `ApiResponse<T>` 래핑. 클래스 레벨 `@Transactional(readOnly=true)`, 쓰기 메서드 override.

**테스트 패턴:**
- 영속성: `@DataJpaTest @AutoConfigureTestDatabase(replace=NONE) @Import(AuditingConfig.class)` + `extends AbstractMysqlContainerTest`.
- 유스케이스: `@ExtendWith(MockitoExtension.class)` + 포트 mock (BDD).
- 컨트롤러: `@WebMvcTest(...)` + `@Import({SecurityConfig, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler})` + `@MockBean` 유스케이스.

**새 패키지:**
```
backend/.../playlist/   (확장)
backend/.../share/      (신규 컨텍스트: interfaces/application/application.port/infra)
renderer/               (신규: Node + Playwright 렌더러)
frontend/src/...        (공개 뷰 + 공유 시트)
```

---

## File Map

| File | Action |
|------|--------|
| `db/migration/V4__add_playlist_share_code.sql` | Create |
| `common/util/ShareCodeGenerator.java` (+test) | Create |
| `common/exception/ErrorCode.java` | Modify (코드 3개) |
| `playlist/infra/persistence/PlaylistEntity.java` | Modify (shareCode) |
| `playlist/domain/Playlist.java` | Modify (shareCode) |
| `playlist/infra/persistence/PlaylistJpaRepository.java` | Modify (findByShareCode) |
| `playlist/application/port/PlaylistQueryRepository.java` | Modify |
| `playlist/application/dto/result/PlaylistCardData.java` | Create |
| `playlist/infra/persistence/PlaylistQueryRepositoryImpl.java` | Modify (카드 데이터) |
| `playlist/application/EnsureShareCodeUseCase.java` (+test) | Create |
| `playlist/application/GetPlaylistCardDataUseCase.java` (+test) | Create |
| `share/application/port/CardRendererPort.java` | Create |
| `share/application/port/CardCachePort.java` | Create |
| `share/application/RenderPlaylistCardUseCase.java` (+test) | Create |
| `share/infra/HttpCardRenderer.java` | Create |
| `share/infra/FileCardCache.java` | Create |
| `share/interfaces/ShareCardController.java` (+WebMvcTest) | Create |
| `share/interfaces/OgSsrController.java` (+WebMvcTest) | Create |
| `common/security/SecurityConfig.java` | Modify (permitAll) |
| `renderer/{package.json,server.js,template.html,Dockerfile}` | Create |
| `docker-compose.prod.yml` | Modify (renderer + 볼륨 + env) |
| `frontend/nginx.conf` | Modify (UA 분기, /share 프록시) |
| `frontend/src/router/index.js` | Modify (/p/:code) |
| `frontend/src/views/PublicPlaylistView.vue` | Create |
| `frontend/src/components/ShareSheet.vue` | Create |
| `frontend/src/views/PlaylistView.vue` | Modify (공유 버튼) |
| `frontend/src/api/playlists.js` | Modify |

---

## Task 1 — 단축코드 기반: V4 + ShareCodeGenerator + Entity/Domain

**산출물:** share_code 컬럼, 코드 생성기, 엔티티/도메인 확장, `findByShareCode`.

- [ ] **Step 1: V4 마이그레이션 작성**
```sql
-- backend/src/main/resources/db/migration/V4__add_playlist_share_code.sql
ALTER TABLE playlists
  ADD COLUMN share_code CHAR(10) NULL UNIQUE AFTER is_public;
```

- [ ] **Step 2: `ShareCodeGeneratorTest` 작성 (실패 먼저)** — `common/util`
  - 검증: 길이 10, 문자집합 `[A-Za-z0-9]`만, 1000회 호출 시 중복 없음.

- [ ] **Step 3: `ShareCodeGenerator` 구현**
```java
package com.vocaloidarchive.common.util;

import java.security.SecureRandom;

public final class ShareCodeGenerator {
  private static final char[] ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
  private static final int LEN = 10;
  private static final SecureRandom RNG = new SecureRandom();
  private ShareCodeGenerator() {}

  public static String generate() {
    StringBuilder sb = new StringBuilder(LEN);
    for (int i = 0; i < LEN; i++) sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
    return sb.toString();
  }
}
```

- [ ] **Step 4: `PlaylistEntity` 확장** — `share_code` 매핑 + 발급 메서드
```java
@Column(name = "share_code", length = 10, unique = true)
private String shareCode;

public void assignShareCode(String code) { this.shareCode = code; }
```

- [ ] **Step 5: `Playlist` 도메인 + `PlaylistEntityMapper` 확장** — `shareCode` 필드를 `reconstitute`/매핑에 추가 (기존 mapper 시그니처에 끼워넣기).

- [ ] **Step 6: `PlaylistJpaRepository`에 조회 추가**
```java
Optional<PlaylistEntity> findByShareCode(String shareCode);
boolean existsByShareCode(String shareCode);
```

- [ ] **Step 7: 빌드 + 마이그레이션 검증**
```
cd backend && ./gradlew compileJava && ./gradlew test --tests "com.vocaloidarchive.common.util.ShareCodeGeneratorTest"
```
Flyway는 앱 기동 시 V4 적용 — 로컬 docker DB로 `bootRun` 1회 확인 (선택).

- [ ] **Step 8: Commit** — `feat(playlist): add share_code column, generator, entity mapping (V4)`

---

## Task 2 — 카드 데이터 조회 (infra 쿼리 확장)

**산출물:** `PlaylistCardData` 결과 DTO + shareCode로 카드 데이터(곡 mood·썸네일, 대표 캐릭터+색, likeSum) 조회.

- [ ] **Step 1: `PlaylistCardData` result record 작성** — `playlist/application/dto/result`
```java
public record PlaylistCardData(
    Long id, String shareCode, String title, String ownerUsername,
    int songCount, long likeSum, String themeColorHex, String primaryCharName,
    boolean isPublic,
    List<SongLine> songs) {            // orderIndex 순 전체 (콜라주 4 + 트랙 3은 소비측에서 slice)
  public record SongLine(Long songId, String title, String thumbnailUrl, String mood) {}
}
```

- [ ] **Step 2: `PlaylistQueryRepository` 포트에 메서드 추가**
```java
Optional<PlaylistCardData> findCardDataByShareCode(String shareCode);
```

- [ ] **Step 3: `PlaylistQueryRepositoryImplTest` (@DataJpaTest) 작성 (실패 먼저)**
  - 셋업: User, Character 2종(색 다름), Song 5곡(다른 mood/캐릭터), playlist + playlist_songs, likes 직접 INSERT.
  - 검증: songs orderIndex 순 / `themeColorHex` = 최빈 캐릭터 색 / 동률 시 character.id 작은 쪽 / 캐릭터 없는 플리 → `#39C5BB` / `likeSum` = 곡 좋아요 합 / 비공개 플리도 조회는 됨(권한은 use case 책임) / 없는 code → empty.

- [ ] **Step 4: `PlaylistQueryRepositoryImpl.findCardDataByShareCode` 구현**
  - `jpa.findByShareCode(code)` → 없으면 `Optional.empty()`.
  - 곡 라인: `songJpa.findWithSongByPlaylistId(id)` → `SongLine(songId, title, resolveThumbnailUrl(...), song.getMood().name())`.
  - 대표 캐릭터: 곡들의 캐릭터를 모아 빈도 집계 → 최빈(동률 id asc). 곡 캐릭터 접근 경로가 없으면 전용 쿼리 추가:
    ```sql
    -- 의사: 플리 곡들의 캐릭터별 등장 수 + 색
    SELECT c.id, c.name, c.color_hex, COUNT(*) cnt
    FROM playlist_songs ps
    JOIN song_characters sc ON sc.song_id = ps.song_id
    JOIN characters c ON c.id = sc.character_id
    WHERE ps.playlist_id = :pid
    GROUP BY c.id, c.name, c.color_hex
    ORDER BY cnt DESC, c.id ASC
    LIMIT 1
    ```
    결과 없으면 name=null, color=`#39C5BB`.
  - likeSum:
    ```sql
    SELECT COALESCE(COUNT(l.song_id),0)
    FROM playlist_songs ps JOIN likes l ON l.song_id = ps.song_id
    WHERE ps.playlist_id = :pid
    ```
  - `songCount` = 곡 라인 수.
  - (구현 방식은 QueryDSL 또는 네이티브/JPQL 자유 — 기존 Impl 컨벤션 따름)

- [ ] **Step 5: 테스트 그린 + 전체 회귀**
```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.infra.persistence.PlaylistQueryRepositoryImplTest"
cd backend && ./gradlew test
```

- [ ] **Step 6: Commit** — `feat(playlist): add card-data query (mood, theme character, like sum) by share code`

---

## Task 3 — 공유 발급 + 카드 데이터 유스케이스 + ErrorCode

**산출물:** lazy 발급, 카드 데이터 조립(권한 포함), 에러코드.

- [ ] **Step 1: `ErrorCode` 3개 추가**
```java
PLAYLIST_NOT_PUBLIC(HttpStatus.BAD_REQUEST, "공개 플레이리스트만 공유할 수 있습니다"),
SHARE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공유 코드 생성에 실패했습니다"),
CARD_RENDER_FAILED(HttpStatus.BAD_GATEWAY, "카드 이미지 생성에 실패했습니다"),
```

- [ ] **Step 2: `EnsureShareCodeUseCaseTest` (Mockito) 작성 (실패 먼저)**
  - 신규 발급(저장 호출) / 기존 코드 재사용(미저장) / 비소유자 → FORBIDDEN / 비공개 → PLAYLIST_NOT_PUBLIC / 충돌 시 재시도 후 성공 / 5회 실패 → SHARE_CODE_GENERATION_FAILED.

- [ ] **Step 3: `EnsureShareCodeUseCase` 구현** (`@Transactional`)
  - findById → 없으면 PLAYLIST_NOT_FOUND. 소유자 검증(securityUtil). `isPublic` 아니면 PLAYLIST_NOT_PUBLIC.
  - shareCode 있으면 그대로 반환. 없으면 `ShareCodeGenerator.generate()` → `existsByShareCode` 체크 → assign → save. UNIQUE 충돌(`DataIntegrityViolationException`) catch 후 재시도(최대 5). 반환: `{ shareCode, shareUrl }` (shareUrl 조립은 컨트롤러에서 host로).

- [ ] **Step 4: `GetPlaylistCardDataUseCaseTest` (Mockito) 작성 (실패 먼저)**
  - public 카드 데이터 반환 / 비공개 → 조회 막힘(공개 전용이므로 not found 처리) / collage 4·tracklist 3 slice는 소비측(렌더 use case/컨트롤러)이라 여기선 전체 반환 확인.

- [ ] **Step 5: `GetPlaylistCardDataUseCase` 구현** (`readOnly`)
  - `queryRepo.findCardDataByShareCode(code)` → empty거나 `!isPublic` → `BusinessException(PLAYLIST_NOT_FOUND)` (존재/공개 노출 회피). 그대로 반환.

- [ ] **Step 6: 테스트 그린 + 회귀**
```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.application.*"
cd backend && ./gradlew test
```

- [ ] **Step 7: Commit** — `feat(playlist): add ensure-share-code & card-data use cases + error codes`

---

## Task 4 — 렌더러 컨테이너 (Node + Playwright)

**산출물:** `renderer/` — 카드 JSON → 1080×1080 PNG. (백엔드와 독립적으로 단독 테스트 가능)

- [ ] **Step 1: `renderer/package.json`**
```json
{ "name": "voca-card-renderer", "private": true, "type": "commonjs",
  "scripts": { "start": "node server.js" },
  "dependencies": { "playwright": "^1.44.0" } }
```

- [ ] **Step 2: `renderer/template.html`** — `card_design_deco.html`의 카드 마크업을 베이스로, 데이터 주입형으로 변환
  - 루트 `.card.deco`(1080×1080)에 `--theme` 변수.
  - `<script id="card-data" type="application/json">__DATA__</script>` 슬롯 + 인라인 스크립트로 DOM 채움: 제목(2줄), `by @owner`, collage 4타일(`background-image:url(thumbnailUrl)`, 부족 시 그라데이션), tracklist 3(title + mood 배지), footer 칩(대표캐릭터·♪songCount·♥likeSum·shareUrl), `document.documentElement.style.setProperty('--theme', themeColorHex)`.
  - 폰트: `fonts/`의 @font-face로 self-host(오프라인 안전) + `fonts-noto-cjk`/emoji는 OS. (CDN `<link>`는 폴백)

- [ ] **Step 3: `renderer/server.js`**
```js
const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const TEMPLATE = fs.readFileSync(path.join(__dirname, 'template.html'), 'utf8');
let browser;

async function render(data) {
  const html = TEMPLATE.replace('__DATA__', JSON.stringify(data).replace(/</g, '\\u003c'));
  const page = await browser.newPage({ viewport: { width: 1080, height: 1080 }, deviceScaleFactor: 1 });
  try {
    await page.setContent(html, { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    const el = await page.$('.card');
    return await el.screenshot({ type: 'png' });
  } finally { await page.close(); }
}

const server = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/health') { res.writeHead(200).end('ok'); return; }
  if (req.method === 'POST' && req.url === '/render') {
    let body = '';
    req.on('data', c => body += c);
    req.on('end', async () => {
      try {
        const png = await render(JSON.parse(body));
        res.writeHead(200, { 'Content-Type': 'image/png' }).end(png);
      } catch (e) { console.error(e); res.writeHead(500).end(String(e)); }
    });
    return;
  }
  res.writeHead(404).end();
});

(async () => {
  browser = await chromium.launch({ args: ['--no-sandbox'] });
  server.listen(3000, () => console.log('renderer on :3000'));
})();
```

- [ ] **Step 4: `renderer/Dockerfile`**
```dockerfile
FROM mcr.microsoft.com/playwright:v1.44.0-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
      fonts-noto-cjk fonts-noto-color-emoji && rm -rf /var/lib/apt/lists/*
COPY package.json ./
RUN npm install --omit=dev
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

- [ ] **Step 5: 로컬 단독 테스트**
```
cd renderer && npm install && node server.js &
curl -s -X POST localhost:3000/render -H 'Content-Type: application/json' \
  -d '{"title":"새벽 세시의 미쿠","ownerUsername":"haku_39","songCount":24,"likeSum":1200,"shareUrl":"voca.archive/p/abc","themeColorHex":"#39C5BB","primaryCharName":"하츠네 미쿠","collage":[{"title":"夜明け","thumbnailUrl":""}],"tracklist":[{"title":"テオ","mood":"ENERGETIC"}]}' \
  --output /tmp/card_test.png && file /tmp/card_test.png   # PNG 1080x1080 기대
```
(모델이 Read로 결과 확인 — 유저 표시는 §유저 검증 참고)

- [ ] **Step 6: Commit** — `feat(renderer): add Node+Playwright card renderer container`

---

## Task 5 — 카드 PNG 엔드포인트 (포트 + 어댑터 + 캐시 + 유스케이스 + 컨트롤러)

**산출물:** `GET /api/share/playlists/{code}/card.png` (캐시→렌더러), `GET /api/share/playlists/{code}` 데이터, `POST /api/playlists/{id}/share`.

- [ ] **Step 1: 포트 정의** — `share/application/port`
```java
public interface CardRendererPort { byte[] render(PlaylistCardData data); }   // 실패 시 BusinessException(CARD_RENDER_FAILED)
public interface CardCachePort {
  Optional<byte[]> get(String code, String contentHash);
  void put(String code, String contentHash, byte[] png);
}
```

- [ ] **Step 2: `RenderPlaylistCardUseCaseTest` (Mockito)** — 캐시 히트(렌더러 미호출) / 미스(렌더러 호출 후 put) / 렌더러 예외 → CARD_RENDER_FAILED.

- [ ] **Step 3: `RenderPlaylistCardUseCase` 구현**
  - `GetPlaylistCardDataUseCase`로 데이터 확보(공개 검증 포함).
  - contentHash = SHA-256(`title|isPublic|themeColorHex|[songId,thumb,mood…순서]`).
  - `cache.get(code,hash)` 히트 반환. 미스 → collage 4/tracklist 3 slice → `renderer.render(...)` → `cache.put` → 반환.

- [ ] **Step 4: 어댑터 구현**
  - `HttpCardRenderer` (infra): `RestClient`로 `${RENDERER_BASE_URL}/render` POST, 8s 타임아웃, 비2xx/예외 → `CARD_RENDER_FAILED`. `RENDERER_BASE_URL`은 `@Value`.
  - `FileCardCache` (infra): `${CARD_CACHE_DIR:/var/cache/cards}` 아래 `{code}_{hash}.png` 읽기/쓰기. 디렉토리 없으면 생성.

- [ ] **Step 5: `ShareCardController` (interfaces)**
  - `POST /api/playlists/{id}/share` (인증) → `EnsureShareCodeUseCase` → `{ shareCode, shareUrl }` (shareUrl = `https://{host}/p/{code}`, host는 `ServletUriComponentsBuilder` 또는 설정값).
  - `GET /api/share/playlists/{code}` → `GetPlaylistCardDataUseCase` → 공개 데이터 응답 record (ApiResponse 래핑).
  - `GET /api/share/playlists/{code}/card.png` → `RenderPlaylistCardUseCase` → `ResponseEntity<byte[]>` `image/png` + `Cache-Control: public, max-age=300`.

- [ ] **Step 6: `SecurityConfig` permitAll** — `/api/share/**`, `/share/**` 추가. `POST /api/playlists/*/share`는 인증 유지(`/api/share/**`와 경로 다름 주의).

- [ ] **Step 7: `ShareCardControllerTest` (@WebMvcTest)** — share POST 인증/비소유자403/비공개400, card.png 200 image/png, 데이터 200, 비공개·없는 code 404.

- [ ] **Step 8: 그린 + 회귀**
```
cd backend && ./gradlew test --tests "com.vocaloidarchive.share.*" && ./gradlew test
```

- [ ] **Step 9: Commit** — `feat(share): add card png endpoint with renderer port, cache, share-code issue`

---

## Task 6 — OG SSR + nginx UA 분기 + compose

**산출물:** 봇용 OG HTML, nginx 라우팅, renderer 서비스 기동.

- [ ] **Step 1: `OgSsrController` (interfaces)** — `GET /share/p/{code}` → `text/html`
  - `GetPlaylistCardDataUseCase`로 공개 데이터(없으면 404 HTML). 메타: `og:title`(제목·VocaloidArchive), `og:description`(`{owner}의 보카로 플레이리스트 · {n}곡`), `og:image`(`https://{host}/api/share/playlists/{code}/card.png`), `og:url`(`/p/{code}`), `twitter:card=summary_large_image`, canonical. HTML escape 처리.

- [ ] **Step 2: `OgSsrControllerTest` (@WebMvcTest)** — OG 메타 포함 200 HTML, 비공개/없음 404.

- [ ] **Step 3: `frontend/nginx.conf` 수정**
```nginx
# 봇이면 백엔드 OG SSR, 사람이면 SPA
location /p/ {
    set $is_bot 0;
    if ($http_user_agent ~* "(Twitterbot|facebookexternalhit|kakaotalk-scrap|Slackbot|Discordbot|TelegramBot|LinkedInBot|WhatsApp|Pinterest)") { set $is_bot 1; }
    if ($is_bot) { proxy_pass http://backend:8080$request_uri; }   # /share/p/{code} 매핑은 백엔드 라우트로
    try_files $uri /index.html;
}
location /share/ { proxy_pass http://backend:8080; }   # OG SSR
# 기존 /api/ 프록시가 /api/share/도 커버
```
  - 주의: `if`+`proxy_pass`는 봇 경로를 `/share/p/{code}`로 보내야 함 → location을 `/p/`로 두되 봇일 때 `proxy_pass http://backend:8080/share/p/...` 형태로 rewrite. (구현 시 정확한 rewrite 규칙 검증 — `rewrite ^/p/(.*)$ /share/p/$1 break;` 후 proxy_pass 권장.)

- [ ] **Step 4: `docker-compose.prod.yml` 수정**
```yaml
  renderer:
    build: { context: ./renderer }
    image: vocaloid-archive-renderer
    container_name: vocaloid-renderer
    restart: unless-stopped
    expose: ["3000"]
  backend:
    environment:
      RENDERER_BASE_URL: http://renderer:3000
      CARD_CACHE_DIR: /var/cache/cards
    volumes:
      - card-cache:/var/cache/cards
# (web의 depends_on에 renderer 불필요 — backend만 호출)
volumes:
  card-cache:
```

- [ ] **Step 5: 로컬 통합 검증 (docker compose)**
```
docker compose -f docker-compose.prod.yml up --build -d
# 봇 UA → OG HTML
curl -s -A "Twitterbot/1.0" http://localhost/p/{code} | grep -i og:image
# 사람 UA → SPA
curl -s -A "Mozilla/5.0" http://localhost/p/{code} | grep -i '<div id="app"'
# 카드 이미지
curl -s http://localhost/api/share/playlists/{code}/card.png --output /tmp/card.png && file /tmp/card.png
```

- [ ] **Step 6: Commit** — `feat(share): add OG SSR endpoint, nginx UA routing, renderer compose service`

---

## Task 7 — 프론트엔드 (공개 뷰 + 공유 버튼)

**산출물:** `/p/:code` 공개 페이지(비로그인) + 가입 CTA, 플리 화면 공유 시트.

- [ ] **Step 1: `api/playlists.js` 확장**
```js
export const ensureShare = (id) => api.post(`/playlists/${id}/share`);          // → { shareCode, shareUrl }
export const getPublicByCode = (code) => api.get(`/share/playlists/${code}`);   // 공개 데이터
export const cardPngUrl = (code) => `/api/share/playlists/${code}/card.png`;
```

- [ ] **Step 2: 라우터에 공개 라우트** — `requiresAuth` 없이
```js
{ path: '/p/:code', name: 'public-playlist',
  component: () => import('@/views/PublicPlaylistView.vue'), meta: { title: '공유 플레이리스트' } },
```

- [ ] **Step 3: `PublicPlaylistView.vue`** — `getPublicByCode(code)`로 곡 목록/제목/대표색 표시(SongCard 재사용). 상단/하단에 **[가입하고 나도 만들기]** CTA(→ `/signup`). 404면 안내 + 홈 링크.

- [ ] **Step 4: `ShareSheet.vue`** — props: `shareUrl`, `code`. 버튼 2개: **[링크 복사]**(`navigator.clipboard`), **[이미지 저장]**(`cardPngUrl(code)` fetch→blob→`a[download]`). 모바일 우선 스타일(데코 톤).

- [ ] **Step 5: `PlaylistView.vue`에 [공유] 버튼** — 클릭 시: 비공개면 "공개로 전환?" 확인 → 공개 PATCH(별도 토글 없으면 안내), `ensureShare(id)` → ShareSheet 오픈.

- [ ] **Step 6: 빌드 확인**
```
cd frontend && npm run build
```

- [ ] **Step 7: Commit** — `feat(frontend): add public playlist page and share sheet`

---

## Task 8 — 통합 검증 & 배포

- [ ] **Step 1: 백엔드 전체 그린** — `cd backend && ./gradlew clean build`
- [ ] **Step 2: 로컬 풀스택 compose 기동** (Task 6 Step 5 재실행), 4서비스(web·backend·renderer + 호스트 DB) 정상.
- [ ] **Step 3: 카드 실측** — 미쿠/루카 테마 플리 각각 `card.png` 받아 모델이 Read로 확인(테마색 분기·콜라주·트랙·footer). **유저 표시는 아래 §유저 검증.**
- [ ] **Step 4: OG 검증** — `curl -A Twitterbot` OG 메타, 일반 UA SPA. (배포 후 Twitter Card Validator)
- [ ] **Step 5: 캐시 동작** — 같은 code 2회 요청 시 2번째가 캐시 히트(렌더러 로그에 1회만).
- [ ] **Step 6: 배포** — main 푸시 → OCI 배포 워크플로. 헬스체크 + 실 트윗 미리보기 확인.
- [ ] **Step 7: 백로그 반영** — `docs/superpowers/backlog.md`에 §10.2 항목 추가, handoff 문서 작성.

---

## §유저 검증 (모바일)

유저는 OCI 원격 + 모바일이라 **Read로 띄운 이미지가 유저 화면에 안 보임**(실측). 카드를 유저에게 보여줄 땐:
- 카드 PNG를 본인 웹서버(포트 80)에서 열 수 있는 URL로 제공하거나(배포 후 `https://{host}/api/share/playlists/{code}/card.png` 자체가 URL),
- 텍스트로 결과 요약.
모델 자신의 디자인 검증용으론 Read 스크린샷 계속 사용 가능.

---

## 리스크 / 주의 (스펙 §7.4 발췌)

- nginx `if` + `proxy_pass`/`rewrite` 조합은 함정 많음 → Task 6 Step 3에서 봇/사람 분기 반드시 curl로 검증.
- 렌더러 폰트: CDN 의존 말고 `fonts/` self-host 권장(오프라인/지연 시 한글·이모지 깨짐 방지).
- 카드 캐시 무효화는 contentHash 기반 — 곡 변경 시 자동. TTL 청소는 후속(백로그).
- 곡 0개 플리: 공유 버튼 비활성(1곡 이상 요구) — Task 7 Step 5에서 가드.
- `POST /api/playlists/{id}/share`(인증) vs `/api/share/**`(공개) 경로 혼동 주의 — SecurityConfig 규칙 정확히.
