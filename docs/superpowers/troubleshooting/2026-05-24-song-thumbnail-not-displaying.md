# 트러블슈팅: 목록에서 곡 썸네일이 안 나오는 문제

- **날짜:** 2026-05-24
- **관련 커밋:** `e6227fe` (fix(song): derive YouTube thumbnail at read time, fall back to empty image)
- **영향 범위:** 홈/검색 곡 그리드(`SongCard`), 플레이리스트 곡 목록(`PlaylistView`)

---

## 증상

직접 DB에 INSERT한 곡(id 2~18)들이 목록 화면에서 썸네일 없이 표시됨. 정상 등록 경로로 만든 곡(id 1)만 썸네일이 보였다.

## 조사

DB를 직접 조회해 상태를 확인:

```sql
SELECT id, title, youtube_url, thumbnail_url FROM songs ORDER BY id;
```

| 패턴 | 곡 | `youtube_url` | `thumbnail_url` |
|------|-----|---------------|-----------------|
| 정상 | 더블레리어트(1) | 있음 | 있음 |
| 깨짐 | 2~11, 13~18 (16곡) | **있음** | **NULL** |
| 유튜브 없음 | 테러(12) | NULL | NULL |

→ `youtube_url`은 있는데 `thumbnail_url`만 NULL. 썸네일이 채워지지 않은 채로 들어왔다는 뜻.

## 근본 원인

썸네일 유도가 **쓰기 시점에서 단 한 곳**(`CreateSongUseCase`)에서만 일어났다.

```java
// CreateSongUseCase.java
String thumbnailUrl = YoutubeUtil.extractThumbnailUrl(cmd.youtubeUrl());
```

곡을 **API/UI 등록 흐름이 아니라 DB에 직접 INSERT**하면 이 로직을 타지 않아 `thumbnail_url`이 NULL로 남는다. 프론트는 `thumbnailUrl`이 없으면 이미지를 그리지 않으므로 썸네일이 비어 보였다.

> 이번 데이터는 Hermes(Telegram) 에이전트가 곡 제목 목록을 받아 `mysql` INSERT로 일괄 적재하면서 발생. 즉 **대량 직접 INSERT 시 파생 컬럼(thumbnail_url)이 비는 것이 재발 가능한 패턴**이다.

## 해결

파생을 **읽기 시점으로 이동**해, 저장값이 없어도 `youtube_url`에서 즉석 유도하도록 변경.

**백엔드**
- `YoutubeUtil.resolveThumbnailUrl(storedThumbnailUrl, youtubeUrl)` 추가: 저장값이 있으면 그대로, 없으면 `extractThumbnailUrl(youtubeUrl)`, 둘 다 없으면 `null`.
- 쿼리 결과 매핑에서 사용:
  - `SongQueryRepositoryImpl#toSongResult` (목록)
  - `SongQueryRepositoryImpl#toSongDetailResult` (상세)
  - `PlaylistQueryRepositoryImpl#findDetailById` (플리 항목)

**프론트엔드**
- `assets/empty-thumb.svg` 플레이스홀더 추가.
- `SongCard.vue` / `PlaylistView.vue`: `thumbnailUrl`이 없으면 빈 이미지, 그리고 `@error`로 죽은 썸네일(삭제된 영상 등) 로드 실패 시에도 빈 이미지로 폴백.
- 기존 캐릭터 자켓(미쿠/루카 .png) 자동 폴백은 요청에 따라 빈 이미지로 통일·제거.

DB 백필은 불필요(읽기 시점 유도이므로 배포 즉시 반영).

## 검증

- `YoutubeUtilTest`에 `resolveThumbnailUrl` 4케이스 추가 → 통과.
- 프로덕션 API `GET /api/songs`: youtube_url 있는 모든 곡이 `https://i.ytimg.com/vi/{id}/hqdefault.jpg` 반환.
- 유도된 썸네일 URL 샘플 5건 `curl` → 전부 HTTP 200(실제 로드됨).
- 라이브 홈 화면 스크린샷에서 16곡 썸네일 표시 확인. 테러(유튜브 없음)는 빈 이미지.

## 참고/주의

- **Testcontainers 레포 테스트**(`*RepositoryTest`)는 일부 러너 환경에서 Docker 초기화 실패(`initializationError`)로 스킵/실패한다 — 코드 결함이 아닌 환경 이슈. 배포 워크플로(`deploy.yml`)엔 테스트 단계가 없어 배포를 막지 않는다.
- 향후 곡을 **직접 INSERT**할 때 썸네일을 영구 저장하고 싶으면, INSERT 시 `thumbnail_url`도 `https://i.ytimg.com/vi/{videoId}/hqdefault.jpg`로 함께 넣을 것. 안 넣어도 화면에는 읽기 시점 유도로 정상 표시된다.
