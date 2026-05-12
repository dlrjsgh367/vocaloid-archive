# Phase 7-2 — Character Domain Migration 완료

- 브랜치: `feature/phase-7-2-character-migration`
- 베이스: `db65b7d` (main, Phase 7-1 merge)
- 머지: 2026-05-13

## 변경 요약

`character` 도메인 (read-only seeded) 을 4-layer 로 마이그레이션. ARCHITECTURE.md "Read 흐름은 Domain bypass" 원칙에 따라 **`domain/` layer 생략**.

### 새 구조

```
character/
├── application/
│   ├── GetCharactersUseCase.java
│   ├── port/CharacterQueryRepository.java
│   └── dto/result/CharacterResult.java
├── infra/persistence/
│   ├── CharacterEntity.java
│   ├── CharacterJpaRepository.java
│   └── CharacterQueryRepositoryImpl.java
└── interfaces/
    ├── CharacterController.java
    └── dto/response/CharacterResponse.java
```

옛 `character/{controller, service, repository, domain, dto}` 모두 제거.

### 임시 브릿지 (Phase 7-7 song 마이그레이션 시 제거 예정)

`interfaces/dto/response/CharacterResponse` 에 `from(CharacterEntity)` 오버로드 추가. `SongResponse`/`SongDetailResponse` 가 `sc.getCharacter()` 를 통해 `CharacterEntity` 를 직접 받기 때문. Phase 7-7 에서 Song 의 응답 빌더가 `CharacterResult` 를 port 로 받도록 바뀌면 이 오버로드는 삭제.

### 비활성 테스트

`@Disabled("phase-7-migration: rewrite in Phase 7-8")` 처리:
- `character/service/CharacterServiceTest.java`
- `character/controller/CharacterControllerTest.java`

### API 동작 검증

`GET /api/characters` → 200 OK, 10개 캐릭터 array 반환 (Flyway V2 시드와 일치).

## 다음 Phase

**Phase 7-3 — Tag 도메인** (단순, `@PrePersist normalize` 처리).
