# Phase 6 Frontend v1 — Final Wave Plan (4 views + polish)

작성일: 2026-05-06
상위 컨텍스트: `docs/superpowers/handoff/2026-05-06-phase-6-frontend-progress.md`

이 플랜은 Phase 6의 잔여 4개 뷰(`SongDetailView`, `SongCreateView`, `SearchView`, `PlaylistView`)와 폴리시(404 라우트, per-route title)를 마무리하기 위한 것이다. 백엔드 실행은 별개 사용자 작업으로 분리하고, 프론트는 빌드(`npm run build`)와 컴포넌트 구조 일관성 검증으로 완료를 선언한다(브라우저 통합 테스트는 사용자 백엔드 기동 후 별도).

---

## API 계약 매핑 (확정)

| Endpoint | Frontend 함수 | Request | Response |
|----------|----------------|---------|----------|
| `GET /api/songs/{id}` | `fetchSong(id)` | — | `SongDetailResponse {id, title, youtubeUrl, niconicoUrl, thumbnailUrl, bpm, mood, playCount, likeCount, registeredBy{id,username}, characters[{id,name,colorHex,imageUrl}], tags[string], createdAt}` |
| `POST /api/songs` | `createSong(body)` | `{title, youtubeUrl?, niconicoUrl?, bpm?, mood, characterIds[Long], tagNames[string]}` | `SongDetailResponse` |
| `DELETE /api/songs/{id}` | `deleteSong(id)` | — | 204 |
| `POST /api/songs/{id}/like` | `toggleLike(id)` | — | `{liked, likeCount}` |
| `GET /api/songs/{id}/comments` | `fetchComments(id, {page,size})` | — | `PageResponse<CommentResponse {id, content, username, createdAt}>` |
| `POST /api/songs/{id}/comments` | `createComment(id, {content})` | `{content}` | `CommentResponse` |
| `DELETE /api/comments/{id}` | `deleteComment(id)` | — | 204 |
| `GET /api/playlists` | `fetchMyPlaylists()` | — | `PlaylistResponse[]` |
| `GET /api/playlists/{id}` | `fetchPlaylist(id)` | — | `PlaylistDetailResponse {id,title,isPublic,ownerUsername, songs[{songId,title,thumbnailUrl,orderIndex}], createdAt}` |
| `POST /api/playlists` | `createPlaylist({title,isPublic})` | `{title, isPublic}` | `PlaylistResponse` |
| `DELETE /api/playlists/{id}` | `deletePlaylist(id)` | — | 204 |
| `POST /api/playlists/{id}/songs` | `addSongToPlaylist(pid, sid)` | `{songId}` | `PlaylistDetailResponse` |
| `DELETE /api/playlists/{id}/songs/{songId}` | `removeSongFromPlaylist(pid, sid)` | — | 204 |

`Mood` enum: `BRIGHT, DARK, EMOTIONAL, ENERGETIC, CALM`. UI 표시는 한국어 라벨 매핑.

`SongCreateRequest` 검증: title(NotBlank, 200자), youtubeUrl(youtube.com|youtu.be 정규식, 500자, optional), niconicoUrl(nicovideo.jp|nico.ms, 500자, optional), bpm(40~300, optional), mood(NotNull), characterIds(NotEmpty, ≤10), tagNames(≤10, 각 1~30자).

---

## 디자인 일관성 규칙

- 색상/폰트/그라디언트는 `global.css` 토큰만 사용 (`--miku-*`, `--pink-*`, `--lav-*`, `--coral-*`, `--font-display`, `--font-jp`).
- 카드/입력 폼은 `LoginView`/`SignUpView`/`SongCard`의 패턴 재사용 (radius, shadow, candy stripe top-bar).
- 버튼 hover에 `transform: translateY(-2px) | rotate(-3deg)` 같은 미세 애니메이션, shimmer/sparkle keyframe 활용.
- 에러 배너는 `var(--coral-*)`, 성공 배너는 `var(--miku-lt)+var(--miku)`.
- 모달은 backdrop blur + glass effect, 닫기 버튼 우상단.

---

## Task 1: SongDetailView (`/songs/:id`)

### 레이아웃
- 상단: YouTube 임베드 (16:9, `https://www.youtube.com/embed/{videoId}`). videoId 추출은 `youtubeUrl`에서 정규식.
- 우측 사이드 (또는 임베드 아래, 반응형): 제목, mood 칩, 캐릭터 배지 행, 등록자, 좋아요 버튼, 통계(play/likes), 태그.
- 본인 곡일 때(`registeredBy.id === authStore.user?.id || ownerUsername === authStore.user?.username`): "삭제" 버튼 (확인 다이얼로그).
- "플레이리스트에 담기" 버튼: 클릭 시 모달 → `fetchMyPlaylists` 결과 목록 + 각 항목 "+추가" → `addSongToPlaylist`.
- 댓글 섹션: 작성 폼(인증 필요, 비로그인 시 placeholder/안내), 목록(페이지네이션, 본인 댓글만 삭제).

### 상태/로직
- `onMounted` → `fetchSong(id)`, `fetchComments(id)` 병렬.
- 좋아요는 HomeView 패턴 재사용 (비로그인 시 `/login?return=/songs/:id`).
- 댓글 작성 후 `comments.unshift(newComment)` (최신순 가정) 또는 재조회.
- 댓글 삭제 후 클라이언트 필터.
- 곡 삭제 후 `router.push('/')`.
- niconico URL 있으면 임베드 대신 외부 링크 버튼만 (politeness).
- youtubeUrl 없을 때 placeholder.

### YouTube videoId 추출
- 정규식: `/(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|v\/))([\w-]{11})/`
- 실패 시 임베드 영역에 "재생할 수 없습니다" 메시지.

### 권한 비교
- 백엔드는 `registeredBy.username` 만 노출 (id 미포함). 본인 판정은 `authStore.user?.username === song.registeredBy.username`.

---

## Task 2: SongCreateView (`/songs/new`, requiresAuth)

### 폼 필드
- title (필수, max 200)
- youtubeUrl (선택, 정규식 검증)
- niconicoUrl (선택)
- bpm (number input, 40~300)
- mood (select / chip group): BRIGHT/DARK/EMOTIONAL/ENERGETIC/CALM
- characterIds: 캐릭터 목록 fetch 후 multi-select 칩 (toggle)
- tagNames: 인풋 + Enter or 쉼표 → 칩 추가, 표시는 칩, 제거 가능. 정규화: `trim().toLowerCase()`. 최대 10개. 중복 제거.

### 검증
- 클라이언트: title NotBlank, characterIds.length ≥ 1.
- youtubeUrl/niconicoUrl 있을 때만 정규식 검증. (둘 다 없어도 OK — 백엔드 모델이 둘 다 optional.)
- 서버 에러는 `error.details?` 있으면 필드별 표시, 없으면 배너.

### 동작
- `onMounted` → `fetchCharacters()`.
- submit → `createSong(body)` → `router.push({ name: 'song-detail', params: { id: result.id } })`.

---

## Task 3: SearchView (`/search`)

### URL ↔ 상태 동기화
- 쿼리 키: `keyword`, `mood`(소문자 또는 대문자, FilterBar 컨벤션 따라 소문자 → MOOD_MAP), `character`(=characterId), `sort`(latest|popular|played), `page`(0-based)
- 초기 마운트 시 `route.query` 읽어서 상태 초기화.
- 상태 변경 시 `router.replace({ query })`로 URL 갱신.
- `watch(() => route.query)` 또는 명시적 `applyFromQuery` 함수 — 단방향 단순화 위해 후자 채택.

### UI
- FilterBar 재사용 (mood + keyword)
- 캐릭터 스트립 (선택된 character 표시) — 있는 character 토글
- 정렬 셀렉터: 최신순/인기순/재생순
- SongGrid + 페이지네이션 (이전/다음 + page 표시)

### 페이지네이션
- size 고정 20.
- `fetchSongs({keyword, mood, characterId, sort, page, size})` 호출.
- 응답 `{content, page, size, totalElements, totalPages}` 사용.
- 첫/마지막 페이지일 때 disable.

---

## Task 4: PlaylistView (`/playlists`, requiresAuth)

### 레이아웃
- 좌측 목록 / 우측 상세 (반응형: 모바일은 단일 컬럼, 상세는 모달).
- "+ 새 플레이리스트" 버튼 → 생성 모달.
- 목록 항목: 제목, 공개/비공개 배지, 곡 수, 생성일.
- 항목 클릭 → 우측에 상세 (or 모달) 노출: 제목, 공개 여부, 곡 목록.
- 상세 곡 카드: 썸네일(있으면), 제목, "곡 보기"(→/songs/{songId}), "곡 빼기"(아이콘 버튼).
- 목록 항목 hover 시 "삭제" 액션 (확인 다이얼로그).

### 동작
- `onMounted` → `fetchMyPlaylists()`.
- 생성 → `createPlaylist({title, isPublic})` → 목록 prepend, 모달 닫음.
- 항목 클릭 → `fetchPlaylist(id)` → 상세 set.
- 곡 제거 → `removeSongFromPlaylist(pid, sid)` → 상세에서 제거 + 곡 수 -1.
- 플레이리스트 삭제 → `deletePlaylist(id)` → 목록에서 제거, 상세 클리어.

---

## Task 5: 폴리시 (`NotFoundView` + per-route title)

- `frontend/src/views/NotFoundView.vue` 추가 (Vocaloid 느낌의 404 일러스트 텍스트, "✦ 찾으시는 곡은 없는 것 같아요" + 홈으로 가기 버튼).
- 라우터에 catch-all `{path: '/:pathMatch(.*)*', name: 'not-found'}`.
- 라우트 `meta.title` 부여 후 `router.afterEach((to) => { document.title = to.meta.title ? `${to.meta.title} · VocaloidArchive` : 'VocaloidArchive'; })`.

---

## Task 6: 검증 + 커밋

- `cd frontend && npm run build` (Vite 빌드 통과 확인).
- 4개 뷰 + NotFound + 라우터 변경 단위로 한 커밋.
  - 메시지: `feat(frontend): implement Phase 6 remaining views (detail/create/search/playlist + 404)`.
- main 직접 작업 중이므로 별도 머지 없이 그대로 커밋.

---

## 비범위 (Out of scope)

- 브라우저 통합 테스트(백엔드 기동이 필요) — 사용자 작업.
- E2E 자동화.
- i18n / dark mode.
- 댓글 수정 (서비스 자체 미지원).
- 플레이리스트 곡 순서 드래그 변경.
- 검색 필터 캐싱/디바운스 라이브러리화.
