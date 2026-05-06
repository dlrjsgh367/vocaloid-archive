# 세션 인수인계 — 2026-05-06 (Phase 6 Frontend 진행 중)

## 작업 배경

Phase 5(백엔드 Playlist) 완료 후 Phase 6 Frontend v1 진입.
claude-design을 통해 오타쿠 감성 디자인 초안(`VocaloidArchive.html`)을 받아 Vue 3 SPA 구현 시작.
홈 화면 + 인증(로그인/회원가입) + API 레이어 + auth store까지 완료. 나머지 4개 뷰는 미완료.

---

## 완료된 작업

### 1. 디자인 토큰 (`frontend/src/assets/styles/global.css`)
- Nunito + Gochi Hand + Zen Maru Gothic 폰트 임포트
- VocaloidArchive 색상 토큰 (miku/pink/lav/yellow/coral 계열)
- 8개 keyframes (shimmer, float-y, sparkle, pulse-glow, drift, blink-cursor 등)
- body에 radial-gradient 멀티 레이어 배경

### 2. App.vue — 헤더 교체
- glassmorphism (backdrop-filter: blur) + candy stripe (4색 반복 그라디언트)
- 로고: `ボカロ図書館` 일본어 부제 + 펄스 글로우 닷
- nav 활성 상태에 floating ♪ 표시
- shimmer 애니메이션 그라디언트 로그인 버튼
- `authStore.isAuthenticated` 분기 (로그인/유저명+로그아웃)

### 3. 컴포넌트 (`frontend/src/components/`)
- `HeroSection.vue` — 22개 파티클 JS 생성, ✦ shimmer 배지, furigana(ruby) 텍스트, stat 카드 3개
- `FilterBar.vue` — 분위기 6칩(전체/밝음/감성/다크/잔잔함/신남) + 검색 인풋, v-model 양방향
- `CharacterStrip.vue` — 캐릭터 pill 스크롤 스트립, 호버 시 ♡ 플로팅
- `SongCard.vue` — 홀로그램 글로우 링, 글리터 오버레이, 캐릭터 색상 동적 매핑
- `SongGrid.vue` — ✦ sparkle 섹션 헤더 + 그리드

### 4. API 레이어 (`frontend/src/api/`)
- `index.js` — JWT 인터셉터 + 401 시 refresh→재시도 (1회 한정, queue로 동시 요청 처리)
- `auth.js` / `songs.js` / `characters.js` / `likes.js` / `comments.js` / `playlists.js`
- 모든 함수가 `response.data.data` 언래핑 (ApiResponse 표준)

### 5. 인증 (`frontend/src/`)
- `stores/auth.js` — login/logout/refresh/clear/_setTokens 액션, refreshToken은 localStorage
- `views/auth/LoginView.vue` — 이메일/비번, return 쿼리 리다이렉트, 가입 직후 알림
- `views/auth/SignUpView.vue` — 닉네임/이메일/비번/확인, 비밀번호 강도 미터(0~4), 약관 체크박스
- 클라이언트 검증 + 서버 에러(`error.message`) 배너 표시

### 6. HomeView.vue — Mock → 실 API
- `fetchCharacters()` → CharacterStrip
- `fetchSongs({keyword, mood, size})` → SongGrid (300ms debounce)
- `toggleLike()` → 비로그인 시 `/login` 리다이렉트
- mood 매핑 표 (`MOOD_MAP`): all/bright/dark/emotional/energetic/calm → BRIGHT/DARK/EMOTIONAL/ENERGETIC/CALM
- 캐릭터명 → colorKey 매핑 (`CHAR_COLOR_KEY_MAP`)

### 7. 백엔드 환경 설정
- `backend/src/main/resources/application-local.yml`에 JWT_SECRET 추가:
  - 값: `dm9jYWxvaWQtYXJjaGl2ZS1sb2NhbC1zZWNyZXQta2V5LW1pbmltdW0tMjU2Yml0cyEh` (Base64, 256bit+)
- `.gitignore`에 `backend/src/main/resources/application-local.yml` 추가
- git 인덱스에서도 제거 (`git rm --cached`) — 커밋 `80429cc`

---

## 확인된 사실 / 분석 결과

### 백엔드 실행 환경 충돌 (중요)

세션 중 백엔드를 띄우려 시도했으나 두 가지 충돌 발견:

1. **8080 포트 점유**: 사용자의 다른 프로젝트인 `rainbowtv-java-api`(`com.cmb.rainbowtv.RainbowtvApplication`)가 IntelliJ 디버그 모드로 실행 중. PID 23872, 17:08:40 시작.
2. **3306 포트 점유**: `my-mysql` 컨테이너가 `bdd-practice` DB로 16시간째 실행 중 (root 비밀번호 `root1234`, vocaloid_archive DB 없음). 우리 docker-compose는 `root/root`을 기대.

→ vocaloid-archive 백엔드를 띄우려면 둘 다 정리 필요 (사용자가 "확인했어"라고 답한 시점에서 해결 방법 제시한 상태였음, 실제 정리는 미확인).

### 디자인 자료 위치
- claude-design 원본 번들: `C:\Users\user\.claude\projects\C--Users-user-workspace-2026-vocaloid-archive\40348e11-f0ab-4a76-b2e9-042ce1e8968c\tool-results\extracted\untitled\`
  - `chats/chat1.md` — 디자인 의도/이력
  - `project/VocaloidArchive.html` — 메인 디자인 (홈)
  - `project/SignUp.html` — 미니멀 스타일 회원가입 (별개 디자인 시스템, 채택 안 함)
- 작업 디렉토리에 복사본: `vocaloid_archive_design.html` (untracked)

---

## 미완료 / 다음 세션 작업

우선순위 순:

### 1. 백엔드 띄우기 (선행)

```powershell
# 옵션 A: 다른 프로젝트 정리 후 우리 것 띄우기
docker stop my-mysql                 # bdd-practice 컨테이너 잠시 정지
Stop-Process -Id <rainbowtv PID> -Force  # 또는 IntelliJ에서 stop

cd backend
docker compose up db -d
./gradlew bootRun --args='--spring.profiles.active=local'

# 옵션 B: 포트 분리 (3307 + 8081) — docker-compose.yml과 application-local.yml 수정 필요
```

### 2. 남은 4개 뷰 구현 (Phase 6 잔여)

모두 mock 데이터 없이 바로 실 API 연동. VocaloidArchive 디자인 토큰 재사용.

| 뷰 | 핵심 기능 |
|----|---------|
| `views/SongDetailView.vue` | YouTube 임베드, 좋아요 토글, 댓글 목록·작성·삭제, 본인 곡 삭제. API: `fetchSong`, `toggleLike`, `fetchComments`, `createComment`, `deleteComment`, `deleteSong` |
| `views/SongCreateView.vue` | 제목/유튜브URL/니코URL/BPM/mood 입력, 캐릭터 다중선택(체크박스), 태그 인풋(쉼표 분리, trim+lowercase). API: `fetchCharacters`, `createSong` |
| `views/SearchView.vue` | URL 쿼리 동기화 (character/mood/keyword/sort/page), 페이지네이션, FilterBar + SongGrid 재사용. API: `fetchSongs` |
| `views/PlaylistView.vue` | 내 플레이리스트 목록, 생성 모달(title/isPublic), 상세(곡 목록·제거), 삭제. API: `fetchMyPlaylists`, `createPlaylist`, `fetchPlaylist`, `deletePlaylist`, `removeSongFromPlaylist` |

### 3. 통합 테스트 (브라우저)

백엔드 연결 후 풀 플로우 확인:
1. `/signup` → `/login` → 홈 → 좋아요 토글
2. `/songs/new` 등록 → 홈에서 새 곡 노출
3. `/songs/:id` 댓글 작성/삭제
4. `/playlists` 생성 → 곡 추가 → 삭제

### 4. 커밋 + 머지

브랜치 전략(CLAUDE.md):
- 현재 `main`에 직접 작업 중 (worktree 미사용)
- Phase 6 단위 커밋 후 push
- 미커밋 파일 19개 (frontend/* 전부 + 디자인 HTML)

---

## 주의사항

### 1. 백엔드 미실행 상태 — 프론트가 401만 받는 중
- 현재 8080은 다른 앱(rainbowtv-java-api)이 응답 중
- vocaloid-archive 백엔드 미실행 → 모든 API 호출이 401/404 받게 됨
- 다음 세션 시작 시 **반드시 백엔드 먼저 정리**

### 2. application-local.yml은 git 추적 대상이 아님
- `.gitignore`에 등록됨 (`80429cc` 커밋)
- JWT_SECRET 포함된 로컬 전용 설정 (커밋 금지)
- 신규 머신에서 클론 시 별도 생성 필요

### 3. CharacterResponse 데이터 한계
- 백엔드 `CharacterResponse`는 `{id, name, colorHex, imageUrl}`만 반환
- 디자인이 요구하는 `jpName`(初音ミク 등), `songCount`는 백엔드 미지원
- HomeView에서 임시로 빈 문자열/0으로 처리 — 추후 백엔드 확장 필요할 수 있음

### 4. 프론트 컴포넌트 — 한국어 캐릭터명 하드코딩 매핑
- `HomeView.vue:CHAR_COLOR_KEY_MAP` — 캐릭터명 문자열로 colorKey 매핑
- 시드 데이터의 정확한 캐릭터명에 의존 (Flyway V2 시드)
- 캐릭터명 변경 시 매핑 깨질 수 있음

### 5. dev 서버 상태
- Vite dev server는 5173에서 실행 중일 수 있음 (PID 11612, 12:10 시작) — 종료 안 함
- 새 세션에서 충돌 시 `Stop-Process -Id 11612 -Force` 또는 그냥 `npm run dev`로 새로 띄움

### 6. 작업 디렉토리 untracked 파일
- `vocaloid_archive_design.html` — claude-design 원본 (참고용, 커밋 여부 미정)
- `docs/superpowers/plans/2026-05-06-phase-5-playlist.md` — 이전 세션 작성, 미커밋
