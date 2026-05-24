# Handoff — 플레이리스트 공유 카드 구현

- 날짜: 2026-05-24
- 브랜치: `feature/share-card` (main 미병합·미배포)
- 스펙: `docs/superpowers/specs/2026-05-24-share-card-design.md`
- 플랜: `docs/superpowers/plans/2026-05-24-share-card-implementation.md`

## 무엇을 했나 (Task 1~7 전부 구현·커밋)

공개 플레이리스트를 데코덴 이미지 카드로 export → 다운로드 + 동적 OG로 SNS 미리보기 → 유입 루프.

| Task | 내용 | 커밋 |
|---|---|---|
| 1 | `share_code`(V4, VARCHAR(10)), `ShareCodeGenerator`, 엔티티/도메인 확장 | `1ed0e62` |
| 2 | shareCode로 카드 데이터 조회(곡 mood·썸네일, 최빈 캐릭터색, likeSum) | `eaa1b5f` |
| 3 | `EnsureShareCodeUseCase`/`GetPlaylistCardDataUseCase` + ErrorCode 3개 | `a98fad3` |
| 4 | 렌더러 컨테이너(Node+Playwright, 데이터 주입 데코 카드) | `a6afb81` |
| 5 | 카드 PNG 엔드포인트(포트·파일캐시·컨트롤러), SecurityConfig permitAll | `7ffb316` |
| 6 | OG SSR(`/share/p/{code}`), nginx 봇 UA 분기, compose renderer 서비스 | `68f69b7` |
| 7 | 프론트(`/p/:code` 공개뷰+가입 CTA, 공유 시트, PlaylistView 공유 버튼) | `4fe2473` |
| infra | Testcontainers↔Docker29 API 픽스 / 렌더러 playwright 핀·node_modules 정리 | `6c217e6`, `6b5068c` |

## 엔드포인트

- `POST /api/playlists/{id}/share` (소유자) → `{ shareCode, shareUrl }`
- `GET /api/share/playlists/{code}` (공개) → 카드 데이터
- `GET /api/share/playlists/{code}/card.png` (공개) → 1080×1080 PNG (contentHash 디스크 캐시)
- `GET /share/p/{code}` (봇 UA만 nginx가 라우팅) → OG 메타 HTML

## 검증 완료 (이 서버에서 안전하게)

- 백엔드 `./gradlew test` 전체 그린 (단위/유스케이스/WebMvc/@DataJpaTest).
- 렌더러 이미지 빌드 + 컨테이너 실행 → `/health` 200, 실제 카드 PNG 1080×1080 생성 확인.
- `docker compose -f docker-compose.prod.yml config` 통과, nginx 문법 통과(파싱), 프론트 `npm run build` 클린.

## ⚠️ 배포 주의 (아직 안 함)

이 OCI 서버가 프로덕션이라, 여기서 `docker compose ... up --build`를 돌리면 **운영 컨테이너 교체 + V4 마이그레이션이 운영 DB에 적용**된다 = 수동 배포. 따라서 전체 스택 통합 검증/마이그레이션은 **main 푸시 → 승인된 배포 워크플로**에서 수행해야 한다.

배포 시 체크:
1. V4 마이그레이션이 운영 `playlists`에 `share_code VARCHAR(10) UNIQUE` 추가.
2. `renderer` 서비스가 새로 뜨고 backend가 `RENDERER_BASE_URL=http://renderer:3000`으로 호출.
3. `card-cache` 볼륨 마운트(`/var/cache/cards`).
4. 실제 트윗/카톡으로 OG 미리보기 확인(또는 Twitter Card Validator). `curl -A Twitterbot https://<host>/p/{code}`로 OG HTML 확인.

## 알려진 한계 / 후속 (backlog 참고)

- 카드 포맷 정사각 1종(og:image 공용). 가로형 1200×630은 백로그.
- 캐시 TTL 청소 없음(contentHash로 자연 무효화만).
- OG `og:image` 절대 URL은 요청 scheme/host 기반(프록시 X-Forwarded-Proto 신뢰). https 전환 시 확인 필요.
- 공유는 public 플리만. 비공개는 프론트에서 안내(공개 전환 토글 엔드포인트는 없음).
