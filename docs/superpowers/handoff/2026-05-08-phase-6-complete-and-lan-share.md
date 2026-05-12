# 세션 인수인계 — 2026-05-08

## 작업 배경

직전 세션(2026-05-06) Phase 6 Frontend 진행 중 핸드오프 이어받기. 잔여 4개 뷰 구현 + 폴리시 일괄 정리 + LAN 공유 시 CORS 차단 해결까지 완료.

---

## 완료된 작업

### 1. Phase 6 코어 — 잔여 4개 뷰 (커밋 `964cd13`)

| 뷰 | 핵심 기능 |
|----|---------|
| `frontend/src/views/SongDetailView.vue` | YouTube 임베드(videoId 정규식 추출), 좋아요 토글, 댓글 페이지네이션·작성·삭제(본인만 표시는 `authStore.isAuthenticated` 기반, 서버 403 처리), 본인 곡 삭제(`authStore.currentUserId === song.registeredBy.id`), 플레이리스트 담기 모달(`fetchMyPlaylists` + `addSongToPlaylist`) |
| `frontend/src/views/SongCreateView.vue` | title/youtubeUrl/niconicoUrl/bpm/mood 필드, 캐릭터 다중선택 칩, 태그 인풋(`,`/Enter, trim+lowercase, 중복제거, 최대 10개), 클라이언트 검증 + 서버 `error.details` 필드 매핑, 성공 시 `/songs/:id`로 push |
| `frontend/src/views/SearchView.vue` | URL 쿼리 ↔ 상태 양방향 동기화 (`mood/keyword/character/sort/page`), 키워드 300ms 디바운스, `route.query` 변화 watch로 back/forward 대응, 페이지 size 20 고정, 캐릭터 pill 토글, 정렬 셀렉터(LATEST/POPULAR/PLAYED) |
| `frontend/src/views/PlaylistView.vue` | 좌측 목록 / 우측 상세 그리드(모바일 단일컬럼), 생성 모달(title + isPublic), 곡 제거(`removeSongFromPlaylist`), 플레이리스트 삭제, 빈 상태 카드 |
| `frontend/src/views/NotFoundView.vue` | 404 catch-all (`/:pathMatch(.*)*`), Vocaloid 감성 일러스트 텍스트 |

기타 변경:
- `frontend/src/router/index.js`: catch-all 라우트 + `meta.title` + `router.afterEach`로 `document.title` 자동 갱신.
- `frontend/src/stores/auth.js`: `currentUserId` getter 추가 (JWT subject 디코딩).

### 2. 폴리시 — backlog.md 7항목 일괄 정리 (커밋 `2e622fe`, `c69c623`)

- **Prettier 3.3** 도입: `frontend/.prettierrc`, `.prettierignore`, `package.json`에 `format` / `format:check` 스크립트. 기존 14개 파일 일괄 포맷.
- **CSS 구조 분할**: `frontend/src/assets/styles/global.css` 삭제 → `tokens.css`(폰트 + 변수 + keyframes) + `reset.css`(`*` reset + body). `main.js`에서 둘 다 import.
- **콜드 스타트 부트스트랩**: `frontend/src/main.js`에서 `app.mount()` 전에 `refreshToken`이 있으면 `auth.refresh()` 호출. 이전엔 새로고침하면 access token 비어있어서 `requiresAuth` 라우트가 즉시 `/login`으로 튕기던 문제.
- **크로스탭 로그아웃 동기화**: `auth.bindCrossTabSync()` 추가. `storage` 이벤트 리스닝하여 다른 탭에서 `refreshToken`이 제거되면 현재 탭도 클리어.
- **`frontend/.env.example`**: `VITE_API_BASE_URL=http://localhost:8080/api` 문서화.
- **favicon**: `frontend/index.html`의 `/favicon.ico` 링크를 SVG 데이터 URL로 교체 (404 제거).
- **`backend/src/main/resources/application.yml`** JWT_SECRET 기본값 fix: `please-change-me-in-prod-with-256bit-random-secret-value`(Base64 디코딩 실패) → `cGxlYXNlLWNoYW5nZS1tZS12b2NhbG9pZC1hcmNoaXZlLTI1NmJpdC1kZXZlbG9wbWVudC1kZWZhdWx0LW9ubHk=`(65 bytes 디코딩 OK). env 주입 없이 `bootRun` 가능.
- **dead code 제거**: `SearchView.vue`의 사용 안 되는 `MOOD_REVERSE`.

### 3. LAN 공유 CORS 해결 (미커밋)

같은 와이파이의 다른 노트북에서 `http://192.168.1.249:5173` 접근 시 "Invalid CORS request" 발생. Vite 프록시는 Origin 헤더를 그대로 포워딩하므로 백엔드까지 도달 → Spring CORS 필터에서 거절.

**`backend/src/main/resources/application-local.yml`**에 `app.cors.origins` 추가:
```yaml
app:
  cors:
    origins: http://localhost:5173,http://192.168.1.249:5173
```

이 파일은 `.gitignore`에 등록돼 있어 커밋되지 않음. **백엔드 재시작 필요**(아직 재시작 안 한 상태일 가능성 높음).

### 4. 문서

- `docs/superpowers/plans/2026-05-06-phase-6-frontend-final.md` — API 계약 매핑 + 4 뷰 상세 스펙.
- `docs/superpowers/handoff/2026-05-06-phase-6-frontend-progress.md` — 직전 세션 핸드오프 (이미 커밋됨).
- 이 문서.

---

## 확인된 사실 / 분석 결과

### 호스트 네트워크 환경
- Wi-Fi 어댑터 IPv4: **`192.168.1.249`** (게이트웨이 `192.168.1.1`).
- OpenVPN(`10.8.0.7`), WSL Hyper-V(`172.17.192.1`)도 떠 있음 — LAN 공유엔 무관.
- VPN이 켜져 있으면 라우팅이 꼬여서 LAN 친구가 못 들어올 수 있음 (해결 시 VPN off).

### CORS 흐름 (LAN 공유 시)
1. 친구 브라우저 `http://192.168.1.249:5173`에서 페이지 로드 → JS가 `/api/...` 호출 (`api/index.js`의 `import.meta.env.VITE_API_BASE_URL || '/api'` 폴백).
2. Vite 프록시(`vite.config.js`)가 `localhost:8080`으로 포워딩. **`changeOrigin: true`는 Host 헤더만 바꾸고 Origin 헤더는 유지**.
3. Spring CORS 필터가 `Origin: http://192.168.1.249:5173` 검사 → 허용 origin 목록에 없으면 "Invalid CORS request" 응답.
4. → application-local.yml에 IP origin 추가하여 해결.

### Vite는 이미 LAN 친화 세팅
- `frontend/package.json` `dev: "vite --host 0.0.0.0"` — 외부 인터페이스 바인딩.
- `frontend/vite.config.js`에 `/api` 프록시 → `localhost:8080` 이미 구성됨.
- 즉 추가 코드 변경 없이 다른 기기에서 접속 가능 (CORS와 방화벽만 통과하면).

### 본인 댓글 판정 한계 (v1 backlog)
- `CommentResponse {id, content, username, createdAt}` — userId 없음.
- JWT subject는 userId만 노출.
- 현재 프론트는 인증된 모든 사용자에게 댓글 삭제 버튼을 보이고, 서버 403 시 alert. 깔끔한 해결은 백엔드 `/me` 엔드포인트 추가 또는 `CommentResponse`에 userId 포함.

---

## 미완료 / 다음 세션 작업

### 우선순위 1 — 백엔드 재시작 후 LAN 공유 검증
1. 백엔드 중지 후 `./gradlew bootRun --args='--spring.profiles.active=local'`.
2. 친구 노트북에서 `http://192.168.1.249:5173` 재시도 — CORS 통과 확인.
3. 통과 안 하면 **Windows 방화벽 인바운드 룰** 점검:
   ```powershell
   New-NetFirewallRule -DisplayName "Vite 5173" -Direction Inbound -Protocol TCP -LocalPort 5173 -Action Allow -Profile Private
   ```
   (관리자 PowerShell, 와이파이 프로필이 "공용"이면 "개인"으로 변경 필요)

### 우선순위 2 — `git push`
- `main`이 `origin/main`보다 4 커밋 앞섬 (`80429cc`, `964cd13`, `2e622fe`, `c69c623`).
- 푸시 타이밍 결정.

### 우선순위 3 — 미결정 항목
- `vocaloid_archive_design.html` (루트 untracked, claude-design 원본): 커밋 vs `.gitignore` 추가 결정.
- 본인 댓글 판정 UX 개선 (위 `확인된 사실` 참고).

### 우선순위 4 — Phase 7 후보 (스코프 외)
- 운영 배포 (OCI, nginx SPA fallback, 컨테이너 비root, 이미지 digest 핀, 백엔드 layered jar)
- ESLint + TypeScript, Vitest, Playwright E2E, CI 파이프라인
- 통계 엔드포인트(`/api/stats`), play_count 스로틀(Redis), maxresdefault 썸네일 폴백
- 캐릭터 `jpName`/`songCount` 백엔드 지원

---

## 주의사항

### 1. `application-local.yml`의 CORS 변경은 미커밋·미반영 상태일 수 있음
- 파일은 수정됐으나 `.gitignore`에 등록돼 있어 git status에 안 나타남.
- 백엔드 재시작 전엔 효과 없음. 커밋 `c69c623` 이후 변경사항은 이게 유일.
- 친구가 못 들어온다면 첫째로 **백엔드 재시작 여부** 확인.

### 2. JWT_SECRET 기본값 변경됨 (`application.yml`)
- 이제 env 주입 없이 `bootRun`이 떠도 죽지 않음.
- **`application-local.yml`의 `app.jwt.secret`이 이 기본값을 override**하는 구조 — 로컬은 여전히 `dm9jYWxv...` 값 사용. 동작에 차이 없음.
- 운영에선 반드시 `JWT_SECRET` 환경변수 주입 필요 (application.yml 주석에 명시).

### 3. dev 서버 + 백엔드 프로세스 상태
- 사용자가 직전에 dev 서버(5173)와 백엔드(8080)를 띄우고 시연 중. 종료 안 함.
- 다음 세션 시작 시 충돌하면 `Get-Process node | Stop-Process` 또는 IntelliJ에서 stop.

### 4. IP가 바뀌면 application-local.yml 다시 수정
- DHCP 재할당, 와이파이 재연결 시 `192.168.1.249`가 바뀔 수 있음.
- 라우터 DHCP에서 노트북 MAC에 고정 IP 할당해두면 편함.

### 5. `vocaloid_archive_design.html` (루트, untracked)
- claude-design 원본 HTML — 디자인 참고용. 빌드와 무관.
- 직전 세션부터 그대로 untracked 상태 유지.

### 6. Prettier 도입 후 EOL 경고
- Windows에서 Prettier가 LF로 통일했으나 Git autocrlf가 CRLF로 자동 변환.
- 커밋 시 LF→CRLF warning 출력은 정상. `.gitattributes` 추가는 별도 결정 사항.

### 7. 메모리(`MEMORY.md`) Phase 상태 업데이트됨
- `project_phase_status.md`에 Phase 6 + 폴리시 커밋(`2e622fe`, `c69c623`) 반영 완료.
- 다음 세션이 메모리만 봐도 현황 파악 가능.
