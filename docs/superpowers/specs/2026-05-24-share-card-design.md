# 플레이리스트 공유 카드 설계 스펙

- 작성일: 2026-05-24
- 상태: 설계 확정 (구현 시작 전)
- 상위 맥락: 유입(acquisition) 최우선 과제 → 플레이리스트를 X/카톡 공유용 이미지 카드로 export
- 의존: Phase 7 아키텍처 마이그레이션 완료 상태 (clean architecture: `interfaces / application / domain / infra`)
- 확정 디자인: `card_design_deco.html` (데코덴 / 카와이 맥시멀리즘, 1080×1080)

---

## 1. 범위

### 1.1 목표

플레이리스트를 **공유 가능한 이미지 카드**로 만들어 X(트위터)·카카오톡 등에 퍼지게 하고, 카드의 돌아오는 링크로 신규 사용자를 유입시킨다.

**유입 루프:** 플리 보유 → [공유] → 카드 생성 → SNS 게시 → 팔로워가 카드 봄 → 링크 클릭 → 공개 플리 페이지 도착 → 가입.

### 1.2 두 가지 공유 메커니즘 (같은 렌더링 로직 공유)

- **A. 다운로드형 카드** — 사용자가 [이미지 저장] 눌러 PNG 받아 직접 트윗에 첨부.
- **B. 동적 OG 이미지** — 공유 링크만 붙여도 SNS가 `og:image`로 미리보기 카드를 자동 렌더. 마찰 최소 → 유입 검증 1순위.

### 1.3 산출물

- DB: `playlists.share_code` 컬럼 추가 (V4 마이그레이션)
- 백엔드: 공유 카드 데이터 조회 + 단축코드 발급 + 카드 PNG 프록시 + 크롤러용 OG SSR 엔드포인트
- 렌더러: **Node + Playwright 헤드리스 크로미움 별도 컨테이너** (HTML→PNG)
- nginx: 크롤러 User-Agent 감지 → OG SSR 프록시 라우팅
- 프론트엔드: 공개 공유 라우트 `/p/:code` + 플리 화면 [공유] 버튼
- docker-compose.prod: `renderer` 서비스 추가

### 1.4 이번 스펙에서 안 하는 것 (백로그)

- 취향 결산 카드 / 곡 추천 카드 (카드 종류 로드맵 2·3순위)
- OG 가로형 1200×630 전용 데코 변형 (MVP는 1080×1080 정사각을 og:image로 공용 → §10 백로그)
- Song.producer 필드 추가 (P명) — 카드 트랙 메타는 mood로 충당
- 카드 디자인 A/B 테스트, 다국어 카드 문구
- 비공개 플리 공유 (공유는 public 플리만)

### 1.5 현재 코드 전제 (확인 완료)

- `playlists` 테이블: `id, user_id, title, is_public, created_at` (단축코드·플리 좋아요·대표 캐릭터 없음)
- `GetPlaylistDetailUseCase`는 **이미 public/private 강제** — public은 익명 조회 허용, private는 소유자만. 공개 페이지 비로그인 노출 요건 이미 충족.
- `PlaylistDetailResult.SongItem` = `(songId, title, thumbnailUrl, orderIndex)` — **mood·캐릭터(colorHex) 없음** → 카드용으로 확장 필요.
- 곡 썸네일은 `YoutubeUtil.resolveThumbnailUrl`로 read-time 유도 (저장값→youtube→null). 카드 콜라주는 이 결과를 그대로 사용.
- 프론트는 nginx `try_files … /index.html` 순수 SPA → OG 메타가 정적. 동적화에 서버 개입 필요.
- 다음 Flyway 버전 = **V4**.

---

## 2. 아키텍처

### 2.1 컴포넌트 추가

```
[브라우저/사람]  --(GET /p/{code})-->  nginx  --(일반 UA)-->  index.html (기존 SPA)
[SNS 크롤러봇]   --(GET /p/{code})-->  nginx  --(봇 UA)----->  Spring: GET /share/p/{code} (OG 메타 HTML)
                                                                       |
                                              og:image = /api/share/playlists/{code}/card.png
                                                                       |
[모두]  --(GET …/card.png)-->  nginx  -->  Spring: 카드 컨트롤러
                                              | (캐시 미스 시)
                                              v
                                         renderer 컨테이너 (Node+Playwright)
                                              | HTML 템플릿 + 카드 데이터 → 1080×1080 PNG
                                              v
                                         Spring: 디스크 캐시 저장 → image/png 응답
```

### 2.2 렌더러를 별도 컨테이너로 두는 이유

데코덴 디자인이 `color-mix()`, `-webkit-text-stroke + paint-order`, `backdrop-filter`, 이모지 스티커, 구글 커스텀 폰트에 의존 → **진짜 브라우저 엔진만 충실 렌더**. 백엔드 내장(Playwright-Java)은 Spring 이미지를 무겁게 만들고, SVG→PNG(Batik)는 데코 효과 재현 불가. 별도 컨테이너면 Spring 이미지는 가볍게 유지하고, 렌더러는 독립적으로 스케일·재시작 가능. (카드 렌더는 결정적 브라우저 동작이라 LLM 토큰 비용 없음.)

### 2.3 OG를 크롤러 UA 감지로 두는 이유

기존 SPA 흐름을 안 건드림. 사람은 그대로 Vue 앱, 봇만 OG 메타 HTML로 분기. 우리가 필요한 건 "링크 붙이면 미리보기"뿐이라 봇만 OG를 읽으면 충분.

---

## 3. 데이터 모델 / 마이그레이션

### 3.1 V4 마이그레이션

```sql
-- V4__add_playlist_share_code.sql
ALTER TABLE playlists
  ADD COLUMN share_code CHAR(10) NULL UNIQUE AFTER is_public;
```

- `share_code`: base62(`A-Za-z0-9`) 10자 랜덤. **NULL 허용** — 첫 공유 요청 시 lazy 발급(백필 불필요). 발급 후 영구 고정.
- UNIQUE 제약. 생성 충돌 시 재시도(최대 5회) 후 실패하면 `BusinessException`.
- 공개 URL: `https://{host}/p/{shareCode}` (숫자 id 노출 회피 + 추측 불가).

### 3.2 카드 데이터 계약 (렌더러로 넘기는 JSON)

```
PlaylistCardData {
  title          : String          // 플리 제목
  ownerUsername  : String          // "by @{username}"
  songCount      : int             // ♪{n}곡
  likeSum        : long            // ♥ = 플리에 담긴 곡들의 좋아요 합
  shareUrl       : String          // 카드 footer 표기용 (예: voca.archive/p/{code})
  themeColorHex  : String          // 대표 캐릭터 color_hex (없으면 #39C5BB 미쿠 기본)
  primaryCharName: String          // 대표 캐릭터명 (footer 칩)
  collage        : [{ title, thumbnailUrl }]   // 상위 4곡 (orderIndex 순)
  tracklist      : [{ title, mood }]           // 상위 3곡
}
```

### 3.3 대표 캐릭터 / 테마 컬러 유도

- 플리에 담긴 모든 곡의 캐릭터를 집계 → **최빈 캐릭터**를 대표로. 동률이면 character.id 오름차순(결정적).
- 대표 캐릭터의 `color_hex` → 카드 `--theme`. 캐릭터가 하나도 없으면 미쿠 기본 `#39C5BB`.
- 기존 `Character` 시드에 `color_hex` 존재(미쿠 #39C5BB, 린 #FFE211, 루카 #FFC0CB, KAITO #1E90FF, MEIKO #E0233F …) → 추가 데이터 없이 구현.

### 3.4 좋아요 합 (footer ♥)

플리 자체엔 좋아요가 없으므로(좋아요는 곡 단위), **플리에 담긴 곡들의 likes 합**을 단건 GROUP BY로 계산해 표기. 의미: "이 플리가 모은 곡들의 인기".

---

## 4. API 명세

전부 base path 유지, 이미지 응답 외에는 `ApiResponse<T>` 래핑.

### 4.1 단축코드 발급 (인증)

| 메서드 | 경로 | 인증 | 동작 |
|---|---|---|---|
| POST | `/api/playlists/{id}/share` | O (소유자) | share_code lazy 발급(이미 있으면 그대로) → `{ shareCode, shareUrl }` 반환 |

- 소유자 아니면 403, public 아니면 `BusinessException(PLAYLIST_NOT_PUBLIC)` (공유는 public만). 비공개를 공유하려 하면 프론트에서 먼저 공개 전환 유도.

### 4.2 공개 카드 데이터 (비인증, public만)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/share/playlists/{code}` | X | 공개 플리 상세 + 카드 메타(title, owner, songCount, likeSum, themeColorHex, primaryCharName, songs). SPA 공개 페이지가 사용 |

- code 없음/비공개 → 404 (존재·공개 여부 노출 회피 위해 통일).

### 4.3 카드 이미지 (비인증, public만)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/api/share/playlists/{code}/card.png` | X | `image/png` 1080×1080. 캐시 히트면 즉시, 미스면 렌더러 호출 후 캐시 |

- `Cache-Control: public, max-age=300`. 캐시 키 = `{code}:{contentHash}` (§7.3).

### 4.4 크롤러용 OG SSR (비인증)

| 메서드 | 경로 | 인증 | 응답 |
|---|---|---|---|
| GET | `/share/p/{code}` | X | OG 메타가 박힌 최소 HTML (`text/html`). nginx가 봇 UA만 여기로 프록시 |

응답 HTML 핵심:
```html
<meta property="og:title"       content="{플리 제목} · VocaloidArchive" />
<meta property="og:description" content="{owner}의 보카로 플레이리스트 · {songCount}곡" />
<meta property="og:image"       content="https://{host}/api/share/playlists/{code}/card.png" />
<meta property="og:url"         content="https://{host}/p/{code}" />
<meta name="twitter:card"       content="summary_large_image" />
<link rel="canonical"           href="https://{host}/p/{code}" />
```
- code 없음/비공개 → 404 HTML.

### 4.5 ErrorCode 추가

```java
PLAYLIST_NOT_PUBLIC(HttpStatus.BAD_REQUEST, "공개 플레이리스트만 공유할 수 있습니다")
SHARE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공유 코드 생성에 실패했습니다")
CARD_RENDER_FAILED(HttpStatus.BAD_GATEWAY, "카드 이미지 생성에 실패했습니다")
```

---

## 5. 렌더러 컨테이너

### 5.1 구성

```
renderer/
  package.json        -- playwright
  server.js           -- HTTP 서버 (POST /render → PNG 바이트)
  template.html       -- card_design_deco.html 기반, 데이터 주입 슬롯
  Dockerfile          -- mcr.microsoft.com/playwright 베이스 (크로미움+의존성 포함)
  fonts/              -- 또는 베이스 이미지에 fonts-noto-cjk / Google 폰트 번들
```

### 5.2 렌더러 API

| 메서드 | 경로 | Body | 응답 |
|---|---|---|---|
| POST | `/render` | `PlaylistCardData` JSON (§3.2) | `image/png` 1080×1080 |
| GET | `/health` | — | 200 |

- 내부 네트워크 전용(`expose`만, 외부 포트 노출 X). Spring만 호출.
- 브라우저 인스턴스 1개 재사용(페이지만 새로 열기) — cold start 회피.
- 폰트: Hachi Maru Pop / Mochiy Pop One / Gamja Flower / Nunito + Noto CJK + Noto Color Emoji 사전 설치. (한글 미지원 디스플레이 폰트 → 한글은 Gamja Flower 폴백 — 기존 발견사항 반영)
- 썸네일은 `i.ytimg.com` 외부 이미지를 렌더러가 직접 로드(서버 사이드라 CORS 무관). 로드 실패 시 빈 타일 폴백.

### 5.3 docker-compose.prod 추가

```yaml
  renderer:
    build: { context: ./renderer }
    image: vocaloid-archive-renderer
    container_name: vocaloid-renderer
    restart: unless-stopped
    expose: ["3000"]
```
- backend 환경변수에 `RENDERER_BASE_URL: http://renderer:3000` 추가.

---

## 6. 핵심 흐름

### 6.1 공유 버튼 (프론트, 소유자)

1. 플리 화면에서 [공유] 클릭.
2. 비공개면 "공개로 전환할까요?" 확인 → 공개 전환.
3. `POST /api/playlists/{id}/share` → `{ shareCode, shareUrl }`.
4. 액션 시트: **[링크 복사]**(`/p/{code}`) · **[이미지 저장]**(`card.png` fetch → download).

### 6.2 OG 미리보기 (봇)

1. 사용자가 트윗에 `https://{host}/p/{code}` 붙임.
2. Twitterbot이 그 URL GET → nginx가 봇 UA 감지 → `/share/p/{code}`로 프록시.
3. Spring이 OG 메타 HTML 반환 (og:image = card.png).
4. Twitterbot이 card.png GET → 캐시/렌더 → 미리보기 카드 표시.

### 6.3 카드 PNG 생성

1. `GET /api/share/playlists/{code}/card.png`.
2. code → public 플리 조회 (비공개/없음 → 404).
3. contentHash 계산 → 캐시 히트면 그 PNG 반환.
4. 미스: 카드 데이터(§3.2) 조립 → 렌더러 `POST /render` → PNG → `{code}:{hash}`로 디스크 캐시 → 반환.

### 6.4 사람의 공개 페이지 도착

1. `https://{host}/p/{code}` (일반 UA) → nginx가 `index.html` 서빙 (SPA).
2. SPA `/p/:code` 라우트 → `GET /api/share/playlists/{code}` → 공개 뷰 렌더 (비로그인 OK). CTA "나도 플리 만들기 → 가입".

---

## 7. 정책 / 캐싱 / 리스크

### 7.1 권한

- 공유·카드·OG는 **public 플리만**. 비공개는 404로 통일(존재 노출 회피).
- share_code 발급은 소유자만. 조회(카드/OG/공개데이터)는 비인증.

### 7.2 nginx 봇 UA 감지

```nginx
location /p/ {
    set $is_bot 0;
    if ($http_user_agent ~* "(Twitterbot|facebookexternalhit|kakaotalk-scrap|Slackbot|Discordbot|TelegramBot|LinkedInBot|WhatsApp)") {
        set $is_bot 1;
    }
    if ($is_bot) { proxy_pass http://backend:8080/share/p/; }
    # 일반 UA는 SPA fallback
    try_files $uri /index.html;
}
```
- `/share/` (백엔드 SSR), `/api/share/` (데이터·이미지)도 `location /api/` 패턴처럼 backend 프록시 추가.

### 7.3 카드 캐시 무효화

- 플리는 `updated_at`이 없으므로 **contentHash** = SHA-256(`title + isPublic + [songId,thumbnailUrl,mood in order] + themeColorHex`).
- 캐시 키 `{code}:{hash}`. 곡 추가/삭제/순서변경 시 hash 바뀌어 자연 무효화. 구 파일은 TTL 청소(예: 7일 미접근).
- 캐시 저장소: MVP는 backend 컨테이너 볼륨(`/var/cache/cards`). 다중 인스턴스 가면 오브젝트 스토리지로(백로그).

### 7.4 리스크 / 완화

| 리스크 | 완화 |
|---|---|
| 렌더러 다운/지연 → 카드 미생성 | 타임아웃(예 8s) + `CARD_RENDER_FAILED` 502. og:image 실패해도 텍스트 OG는 노출 |
| 썸네일 외부 로드 실패 | 렌더러에서 빈 타일 폴백, 전체 렌더는 계속 |
| share_code 충돌 | UNIQUE + 재시도 5회 |
| 봇 UA 누락(새 스크래퍼) | UA 목록 확장 용이. 최악에도 사람은 정상 |
| 곡 0개 플리 공유 | 콜라주/트랙 비면 "빈 플리" 가드 — 공유 버튼 비활성 또는 1곡 이상 요구 |
| 캐시 폭증 | contentHash 키 + TTL 청소 |
| 크로미움 메모리 | 렌더러 단일 브라우저 재사용, 컨테이너 메모리 제한 |

---

## 8. 패키지 / 파일 매핑

### 8.1 백엔드 (clean architecture, playlist 컨텍스트 확장 + share)

| 파일 | Action | 책임 |
|---|---|---|
| `db/migration/V4__add_playlist_share_code.sql` | Create | share_code 컬럼 |
| `playlist/infra/persistence/PlaylistEntity.java` | Modify | `shareCode` 필드 + 발급 메서드 |
| `playlist/domain/Playlist.java` | Modify | `shareCode`, `isPublic` 도메인 표현 |
| `playlist/application/EnsureShareCodeUseCase.java` | Create | lazy 발급(소유자·public 검증) |
| `playlist/application/GetPlaylistCardDataUseCase.java` | Create | 카드 데이터(§3.2) 조립 (대표 캐릭터·likeSum) |
| `playlist/application/port/PlaylistQueryRepository.java` | Modify | `findCardDataByShareCode`, `findPublicDetailByShareCode` |
| `playlist/infra/persistence/PlaylistQueryRepositoryImpl.java` | Modify | 곡별 mood·캐릭터(colorHex) fetch + 최빈 캐릭터 + likeSum GROUP BY |
| `share/interfaces/ShareCardController.java` | Create | `/api/share/playlists/{code}`, `/card.png`, `POST /api/playlists/{id}/share` |
| `share/interfaces/OgSsrController.java` | Create | `GET /share/p/{code}` OG HTML |
| `share/application/RenderPlaylistCardUseCase.java` | Create | 캐시 조회 → 렌더러 호출 → 캐시 저장 |
| `share/application/port/CardRendererPort.java` | Create | 렌더러 추상화 |
| `share/infra/HttpCardRenderer.java` | Create | `RENDERER_BASE_URL` POST /render (RestClient) |
| `share/infra/FileCardCache.java` | Create | contentHash 디스크 캐시 |
| `common/util/ShareCodeGenerator.java` | Create | base62 10자 |
| `common/exception/ErrorCode.java` | Modify | §4.5 코드 3개 |
| `common/security/SecurityConfig.java` | Modify | `/share/**`, `/api/share/**` permitAll |

### 8.2 렌더러 (신규 디렉토리 `renderer/`)

`package.json`, `server.js`, `template.html`, `Dockerfile`, (선택)`fonts/`

### 8.3 프론트엔드

| 파일 | Action | 책임 |
|---|---|---|
| `src/router/index.js` | Modify | `/p/:code` 공개 라우트(비인증) |
| `src/views/PublicPlaylistView.vue` | Create | 공개 플리 뷰 + 가입 CTA |
| `src/views/PlaylistView.vue` | Modify | [공유] 버튼 + 액션시트 |
| `src/components/ShareSheet.vue` | Create | 링크복사 / 이미지저장 |
| `src/api/playlists.js` | Modify | `ensureShare`, `getPublicByCode`, `cardPngUrl` |

### 8.4 인프라

| 파일 | Action |
|---|---|
| `docker-compose.prod.yml` | Modify — `renderer` 서비스 + backend `RENDERER_BASE_URL`, 카드 캐시 볼륨 |
| `frontend/nginx.conf` | Modify — `/p/` 봇 UA 분기, `/share/`·`/api/share/` 프록시 |

---

## 9. 테스트 전략

### 9.1 백엔드 단위/슬라이스

| 대상 | 종류 | 시나리오 |
|---|---|---|
| `ShareCodeGenerator` | 순수 단위 | 길이 10·base62·반복 호출 유일성 |
| `EnsureShareCodeUseCase` | Mockito | 신규 발급 / 기존 재사용 / 비소유자 403 / 비공개 PLAYLIST_NOT_PUBLIC / 충돌 재시도 |
| `GetPlaylistCardDataUseCase` | Mockito | 최빈 캐릭터·동률 tiebreak / 캐릭터 없음 → 미쿠 기본 / likeSum 합산 / collage 4·tracklist 3 cap |
| `PlaylistQueryRepositoryImpl` | `@DataJpaTest`+Testcontainers | shareCode 조회, mood·캐릭터·likeSum 정확 |
| `RenderPlaylistCardUseCase` | Mockito | 캐시 히트(렌더러 미호출) / 미스(렌더러 호출 후 저장) / 렌더러 실패 → CARD_RENDER_FAILED |
| `ShareCardController` | `@WebMvcTest` | public PNG/데이터 200, 비공개 404, share POST 인증/403 |
| `OgSsrController` | `@WebMvcTest` | OG 메타 포함 HTML, 비공개 404 |

### 9.2 렌더러

- `/health` 200, `/render`가 유효 PNG(매직바이트 `\x89PNG`, 1080×1080) 반환, 썸네일 URL 실패 시에도 PNG 생성.

### 9.3 통합 / 수동 검증

- 헤드리스 크로미움으로 실제 카드 렌더 → 스크린샷 인라인 확인 (모바일 유저용, 메모리 [[collab-show-ui-screenshots]] 방식).
- 미쿠/루카 등 캐릭터별 테마 색 분기 확인.
- 봇 UA(`curl -A Twitterbot …/p/{code}`)로 OG HTML, 일반 UA로 SPA 확인.

### 9.4 완료 정의

- `./gradlew clean build` + 신규 테스트 그린, 기존 회귀 없음.
- docker-compose.prod 3→4 서비스 기동, `/p/{code}` 봇/사람 분기 동작.
- 실제 트윗 미리보기에 카드 노출(또는 Twitter Card Validator 통과).
- 카드 1장 실측 스크린샷 사용자 확인.

---

## 10. 결정 이력 / 백로그

### 10.1 확정 결정

- **렌더 엔진**: 헤드리스 크로미움 별도 컨테이너 (vs 백엔드 내장 / SVG→PNG). 이유: 데코 CSS 충실도 + Spring 이미지 경량 유지 + 검증된 파이프라인.
- **OG 전달**: nginx 크롤러 UA 감지 → Spring SSR (vs 항상 SSR). 이유: SPA 흐름 무변경, 마찰 최소.
- **단축코드**: `share_code` 컬럼 + lazy 발급 (vs 숫자 id 노출). 이유: 추측 불가 + 깔끔한 링크.
- **footer ♥**: 플리 곡들의 좋아요 합 (플리 자체 좋아요 없음).
- **대표 캐릭터**: 최빈 + id tiebreak, 없으면 미쿠 기본.
- **카드 포맷**: MVP는 1080×1080 정사각 1종을 다운로드·og:image 공용.

### 10.2 백로그

- **OG 가로형 1200×630 데코 변형** — `summary_large_image` 비율(1.91:1) 최적화. 정사각은 크롭/레터박스됨 → fast-follow.
- **취향 결산 카드 / 곡 추천 카드** — 카드 종류 로드맵 2·3순위.
- **Song.producer(P명) 추가** — 보카로 카드에서 중요. 트랙 메타를 mood→P명으로 격상 검토.
- **카드 캐시 오브젝트 스토리지화** — 다중 인스턴스 대비.
- **공유 클릭/유입 트래킹** — 루프 효과 측정(UTM·리퍼러).

### 10.3 열린 질문 (구현 전 확인)

- 정사각 og:image를 트위터가 어떻게 크롭하는지 실측 → 가로형 백로그 우선순위 조정.
- 카드 캐시 볼륨 위치/TTL 운영값.
- 곡 0~3개 플리의 콜라주/트랙 폴백 레이아웃 디테일 (디자인 후속).
