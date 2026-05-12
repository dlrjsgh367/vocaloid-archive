# Phase 7-7 — Song Domain Migration 완료

- 브랜치: `feature/phase-7-7-song-migration`
- 베이스: `1df568f` (Phase 7-6 merge)
- 머지: 2026-05-13

## 변경 요약

`song` 도메인 (검색·등록·상세·삭제, QueryDSL, 다중 엔티티) 4-layer 마이그레이션 — **Phase 7 의 최대 복잡도**. **Cross-domain port migration** 도 함께 진행 (Comment/Like/Playlist UseCase 가 새 `song.application.port.SongRepository` 의존).

### 새 구조

```
song/
├── domain/
│   ├── Song.java                              # Pure POJO scalar 필드만
│   └── Mood.java                              # enum (stays in domain/)
├── application/
│   ├── SearchSongsUseCase.java
│   ├── GetSongDetailUseCase.java              # incrementPlayCount + detail fetch
│   ├── CreateSongUseCase.java
│   ├── DeleteSongUseCase.java
│   ├── SongSortKey.java                       # application-layer enum
│   ├── port/{SongRepository, SongQueryRepository}.java
│   └── dto/
│       ├── command/CreateSongCommand.java
│       └── result/{SongResult, SongDetailResult}.java  # nested Owner/CharacterRef
├── infra/persistence/
│   ├── SongEntity, SongCharacterEntity, SongTagEntity
│   ├── SongCharacterId, SongTagId
│   ├── SongJpaRepository.java
│   ├── SongQueryRepositoryImpl.java           # QueryDSL impl of port
│   ├── SongRepositoryImpl.java
│   └── SongEntityMapper.java
└── interfaces/
    ├── SongController.java
    └── dto/{request, response}
```

옛 `song/{controller, service, repository, dto}` 모두 제거.

### Cross-domain port migration (Phase 7-7 핵심 cleanup)

다음 UseCase 가 새 `song.application.port.SongRepository.existsById` 로 갱신됨:
- `comment/application/CreateCommentUseCase`, `ListCommentsUseCase`
- `like/application/ToggleLikeUseCase`
- `playlist/application/AddSongToPlaylistUseCase`

각 도메인 `*RepositoryImpl` 는 여전히 `SongJpaRepository` 직접 사용 (`getReferenceById` 용도, infra 차원 cross-domain 결합 — Modular Monolith 후속 과제).

### 발견된 회귀 및 수정

`CreateSongUseCase` 의 응답에 `registeredBy.username = null` 회귀 발견 (구 코드는 `Song.getRegisteredBy().getUsername()` 으로 LAZY fetch). `UserQueryRepository.findSummaryById` 로 username 조회하여 fix commit (`ab006d4`).

### 비활성 테스트

- `song/{service, controller, repository}/*Test.java` 4개 → `@Disabled` 스텁

### API 동작 검증

| 엔드포인트 | HTTP | 결과 |
|---|---|---|
| POST /api/songs (CALM mood, character + tags) | 201 | id=8, owner.username 정상 |
| GET /api/songs/8 | 200 | playCount=1 (increment 동작) |
| GET /api/songs?size=1&sort=LATEST | 200 | 신규 곡 최상위 |
| DELETE /api/songs/8 | 204 | OK |

## 다음 Phase

**Phase 7-8 — Cleanup + 테스트 일괄 재작성** (Phase 7 마지막 단계).
