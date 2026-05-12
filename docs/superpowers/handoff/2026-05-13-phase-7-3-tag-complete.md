# Phase 7-3 — Tag Domain Migration 완료

- 브랜치: `feature/phase-7-3-tag-migration`
- 베이스: `462523d` (Phase 7-2 merge)
- 머지: 2026-05-13

## 변경 요약

`tag` 도메인을 4-layer 로 마이그레이션. Tag 는 API 직접 노출 없음 (Song 등록 시 자동 생성). `interfaces/` 생략.

### 새 구조

```
tag/
├── domain/Tag.java                            # Pure POJO
├── application/
│   ├── FindOrCreateTagsUseCase.java
│   ├── port/TagRepository.java
│   └── dto/result/TagResult.java
└── infra/persistence/
    ├── TagEntity.java
    ├── TagJpaRepository.java
    ├── TagRepositoryImpl.java
    └── TagEntityMapper.java
```

옛 `tag/{service, repository, domain/Tag@Entity}` 모두 제거.

### SongService 브릿지

`SongService.create()` 는 새 `FindOrCreateTagsUseCase` 를 호출하지만, Song 도메인 (Phase 7-7 까지 legacy) 이 여전히 `TagEntity` 를 `addTag()` 로 받기 때문에 `TagJpaRepository.getReferenceById(r.id())` 로 TagResult → TagEntity 변환. Phase 7-7 에서 깔끔히 정리 예정.

### 비활성 테스트

- `tag/service/TagServiceTest.java` — `@Disabled` 스텁
- `tag/repository/TagRepositoryTest.java` — 새 `TagJpaRepository` + `TagEntity` 직접 사용으로 활성 유지
- `song/service/SongServiceTest.java` — Mock 갱신 (`FindOrCreateTagsUseCase` + `TagJpaRepository`), 활성 유지

### API 동작 검증

`POST /api/songs` (tagNames=`["phase7","TEST"," migration "]`) → 201. 응답의 `tags: ["phase7","test","migration"]` — 정규화(`trim().toLowerCase()`) 동작 확인.

## 다음 Phase

**Phase 7-4 — Like 도메인** (격리된 토글 + 복합키).
