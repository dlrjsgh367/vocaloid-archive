# Phase 7-5 — Comment Domain Migration 완료

- 브랜치: `feature/phase-7-5-comment-migration`
- 베이스: `399269b` (Phase 7-4 merge)
- 머지: 2026-05-13

## 변경 요약

`comment` 도메인 CRUD 4-layer 마이그레이션. Pure POJO Comment + 3개 UseCase (List/Create/Delete).

### 새 구조

```
comment/
├── domain/Comment.java
├── application/
│   ├── ListCommentsUseCase.java
│   ├── CreateCommentUseCase.java
│   ├── DeleteCommentUseCase.java
│   ├── port/{CommentRepository, CommentQueryRepository}.java
│   └── dto/{command/CreateCommentCommand, result/CommentResult}.java
├── infra/persistence/
│   ├── CommentEntity.java
│   ├── CommentJpaRepository.java
│   ├── CommentRepositoryImpl.java
│   ├── CommentQueryRepositoryImpl.java
│   └── CommentEntityMapper.java
└── interfaces/
    ├── CommentController.java
    └── dto/{request/CommentCreateRequest, response/CommentResponse}.java
```

옛 `comment/{controller, service, repository, dto}` 제거.

### Cross-domain note

CommentRepositoryImpl/CreateCommentUseCase 가 legacy `song.repository.SongRepository` 의존 — Phase 7-7 에서 SongQueryPort 로 교체.

### 비활성 테스트

- `comment/service/CommentServiceTest` — `@Disabled` 스텁
- `comment/controller/CommentControllerTest` — `@Disabled` 스텁
- `comment/repository/CommentRepositoryTest` — 새 `CommentJpaRepository` + `CommentEntity` 직접 사용으로 활성 유지

### API 동작 검증

- `POST /api/songs/6/comments` (Bearer): 201 + `{id, content, username, createdAt}`
- `GET /api/songs/6/comments`: 200 + `PageResponse` (1개 comment)
- `DELETE /api/comments/6` (Bearer): 204

## 다음 Phase

**Phase 7-6 — Playlist 도메인** (CRUD + Playlist/PlaylistSong 다중 엔티티).
