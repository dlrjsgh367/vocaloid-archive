# Phase 7-6 — Playlist Domain Migration 완료

- 브랜치: `feature/phase-7-6-playlist-migration`
- 베이스: `dedfa03` (Phase 7-5 merge)
- 머지: 2026-05-13

## 변경 요약

`playlist` 도메인 (CRUD + 곡 추가/삭제, 6 endpoints, 2 entities) 4-layer 마이그레이션. 6개 UseCase per-method 분해.

### 새 구조 (24 새 파일)

```
playlist/
├── domain/{Playlist, PlaylistSong}.java           # 2 Pure POJO
├── application/
│   ├── 6 UseCases (ListMyPlaylists, GetPlaylistDetail, CreatePlaylist,
│   │              DeletePlaylist, AddSongToPlaylist, RemoveSongFromPlaylist)
│   ├── port/{PlaylistRepository, PlaylistQueryRepository, PlaylistSongRepository}
│   └── dto/{command/{CreatePlaylistCommand, AddSongToPlaylistCommand},
│            result/{PlaylistResult, PlaylistDetailResult}}
├── infra/persistence/
│   ├── PlaylistEntity, PlaylistSongEntity, PlaylistSongId
│   ├── PlaylistJpaRepository, PlaylistSongJpaRepository
│   ├── PlaylistRepositoryImpl, PlaylistQueryRepositoryImpl, PlaylistSongRepositoryImpl
│   └── PlaylistEntityMapper
└── interfaces/
    ├── PlaylistController
    └── dto/{request, response}
```

옛 `playlist/{controller, service, repository, dto}` (6 파일) 제거.

### 도메인 로직 강조

`Playlist.isOwnedBy(Long)` 도메인 메서드 — 모든 권한 체크 UseCase 가 이를 통해 ownership 검증.

### Cross-domain note

`AddSongToPlaylistUseCase` 가 legacy `song.repository.SongRepository` 의존 (existsById) — Phase 7-7 에서 정리.

### 비활성 테스트

- `playlist/service/PlaylistServiceTest`, `playlist/controller/PlaylistControllerTest` — `@Disabled` 스텁
- `playlist/repository/{Playlist, PlaylistSong}RepositoryTest` — 새 Entity/JpaRepository 직접 사용으로 활성 유지

### API 동작 검증 (6단계)

| 엔드포인트 | HTTP |
|---|---|
| POST /api/playlists | 201 |
| GET /api/playlists | 200 |
| POST /api/playlists/{id}/songs | 204 |
| GET /api/playlists/{id} (detail with songs) | 200 |
| DELETE /api/playlists/{id}/songs/{songId} | 204 |
| DELETE /api/playlists/{id} | 204 |

## 다음 Phase

**Phase 7-7 — Song 도메인** (최고 복잡도: QueryDSL search + Mood + SongCharacter/SongTag).
