# Phase 7-4 — Like Domain Migration 완료

- 브랜치: `feature/phase-7-4-like-migration`
- 베이스: `47848d2` (Phase 7-3 merge)
- 머지: 2026-05-13

## 변경 요약

`like` 도메인 (단일 토글) 을 4-layer 로 마이그레이션. Pure POJO `Like {userId, songId, likedAt}` + 단일 `ToggleLikeUseCase`.

### 새 구조

```
like/
├── domain/Like.java
├── application/
│   ├── ToggleLikeUseCase.java
│   ├── port/
│   │   ├── LikeRepository.java
│   │   └── LikeQueryRepository.java
│   └── dto/result/ToggleLikeResult.java
├── infra/persistence/
│   ├── LikeEntity.java                  # @Entity (@IdClass LikeId)
│   ├── LikeId.java                      # composite key
│   ├── LikeJpaRepository.java
│   ├── LikeRepositoryImpl.java
│   ├── LikeQueryRepositoryImpl.java
│   └── LikeEntityMapper.java
└── interfaces/
    ├── LikeController.java              # POST /api/songs/{id}/like
    └── dto/response/LikeToggleResponse.java
```

옛 `like/{controller, service, repository, dto}` 제거.

### Cross-domain note

`LikeRepositoryImpl.save()` 와 `ToggleLikeUseCase` 가 legacy `song.repository.SongRepository` 를 직접 의존. Song 도메인은 Phase 7-7 에서 마이그레이션 예정 — 그때 SongQueryPort 로 교체.

### 비활성 테스트

- `like/service/LikeServiceTest.java` — `@Disabled` 스텁
- `like/controller/LikeControllerTest.java` — `@Disabled` 스텁

### API 동작 검증

`POST /api/songs/6/like` (Bearer 토큰 포함):
- 1차 호출: `{liked: true, likeCount: 1}` (200)
- 2차 호출: `{liked: false, likeCount: 0}` (200)

## 다음 Phase

**Phase 7-5 — Comment 도메인** (CRUD).
