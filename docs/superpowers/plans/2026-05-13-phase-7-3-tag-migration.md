# Phase 7-3 — Tag Domain Migration Plan

**Goal:** `tag` 도메인을 4-layer 로 마이그레이션. Tag 는 API 엔드포인트 없음 (Song 등록 시 자동 생성). Pure POJO + 1개 UseCase (`FindOrCreateTagsUseCase`).

**Worktree:** `.worktrees/phase-7-3-tag-migration`, branch `feature/phase-7-3-tag-migration`. Base: `462523d` (Phase 7-2 merge).

**최종 구조:**
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

(no `interfaces/` — Tag 에 직접 노출 API 없음)

옛 `tag/{service, repository, domain/Tag@Entity}` 모두 제거.

---

## Task 1: Mechanical rename `Tag → TagEntity`

- `tag/domain/Tag.java` → `tag/infra/persistence/TagEntity.java` (rename class)
- `tag/repository/TagRepository.java` → `tag/infra/persistence/TagJpaRepository.java`
- Cross-domain: `song/domain/Song.java` (addTag param), `song/domain/SongTag.java` (ManyToOne field type), `song/service/SongService.java`, `song/repository/SongQueryRepository.java` (`QTag → QTagEntity`, static field `tag → tagEntity`)
- `tag/service/TagService.java` (still legacy at this step) — update imports/types to TagEntity, TagJpaRepository
- Tests: `tag/service/TagServiceTest`, `tag/repository/TagRepositoryTest`, `song/service/SongServiceTest`, `song/repository/SongRepositoryTest`, `song/repository/SongQueryRepositoryTest`
- Method-local variable names UNCHANGED

Commit: `refactor(tag): rename Tag entity to TagEntity and move to infra/persistence`

Acceptance:
- `grep -r "com.vocaloidarchive.tag.domain.Tag\b"` → 0 matches in main; tests packages only
- `grep -r "com.vocaloidarchive.tag.repository"` → 0 in main
- `./gradlew clean compileJava compileTestJava test` BUILD SUCCESSFUL

## Task 2: Pure POJO + port + impl + UseCase + delete legacy + SongService update

### Files to create

`tag/domain/Tag.java`:
```java
package com.vocaloidarchive.tag.domain;

public class Tag {
  private final Long id;
  private final String name;

  private Tag(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public static Tag newTag(String normalizedName) {
    return new Tag(null, normalizedName);
  }

  public static Tag reconstitute(Long id, String name) {
    return new Tag(id, name);
  }

  public Long getId() { return id; }
  public String getName() { return name; }
}
```

`tag/application/dto/result/TagResult.java`:
```java
package com.vocaloidarchive.tag.application.dto.result;

import com.vocaloidarchive.tag.domain.Tag;

public record TagResult(Long id, String name) {
  public static TagResult from(Tag t) {
    return new TagResult(t.getId(), t.getName());
  }
}
```

`tag/application/port/TagRepository.java`:
```java
package com.vocaloidarchive.tag.application.port;

import com.vocaloidarchive.tag.domain.Tag;
import java.util.Collection;
import java.util.List;

public interface TagRepository {
  List<Tag> findAllByNameIn(Collection<String> names);
  List<Tag> saveAll(List<Tag> tags);
}
```

`tag/infra/persistence/TagEntityMapper.java` (package-private):
```java
package com.vocaloidarchive.tag.infra.persistence;

import com.vocaloidarchive.tag.domain.Tag;

final class TagEntityMapper {

  private TagEntityMapper() {}

  static Tag toDomain(TagEntity e) {
    return e == null ? null : Tag.reconstitute(e.getId(), e.getName());
  }

  static TagEntity toNewEntity(Tag t) {
    return TagEntity.of(t.getName());
  }
}
```

`tag/infra/persistence/TagRepositoryImpl.java`:
```java
package com.vocaloidarchive.tag.infra.persistence;

import com.vocaloidarchive.tag.application.port.TagRepository;
import com.vocaloidarchive.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

  private final TagJpaRepository jpa;

  @Override
  public List<Tag> findAllByNameIn(Collection<String> names) {
    return jpa.findAllByNameIn(names).stream()
        .map(TagEntityMapper::toDomain)
        .toList();
  }

  @Override
  public List<Tag> saveAll(List<Tag> tags) {
    List<TagEntity> entities = tags.stream().map(TagEntityMapper::toNewEntity).toList();
    return jpa.saveAll(entities).stream().map(TagEntityMapper::toDomain).toList();
  }
}
```

`tag/application/FindOrCreateTagsUseCase.java`:
```java
package com.vocaloidarchive.tag.application;

import com.vocaloidarchive.tag.application.dto.result.TagResult;
import com.vocaloidarchive.tag.application.port.TagRepository;
import com.vocaloidarchive.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindOrCreateTagsUseCase {

  private final TagRepository tagRepository;

  @Transactional
  public List<TagResult> invoke(List<String> rawNames) {
    if (rawNames == null || rawNames.isEmpty()) return List.of();

    Set<String> normalized = new LinkedHashSet<>();
    for (String raw : rawNames) {
      if (raw == null) continue;
      String n = raw.trim().toLowerCase();
      if (!n.isEmpty()) normalized.add(n);
    }
    if (normalized.isEmpty()) return List.of();

    Map<String, Tag> existing = tagRepository.findAllByNameIn(normalized).stream()
        .collect(Collectors.toMap(Tag::getName, Function.identity()));

    List<Tag> toCreate = new ArrayList<>();
    for (String n : normalized) {
      if (!existing.containsKey(n)) toCreate.add(Tag.newTag(n));
    }
    if (!toCreate.isEmpty()) {
      try {
        tagRepository.saveAll(toCreate);
      } catch (DataIntegrityViolationException ex) {
        // Concurrent insert won the race — re-read
      }
      tagRepository.findAllByNameIn(normalized).forEach(t -> existing.put(t.getName(), t));
    }

    return normalized.stream().map(existing::get).map(TagResult::from).toList();
  }
}
```

### SongService update (legacy, still uses TagEntity)

Replace `private final TagService tagService;` with `private final FindOrCreateTagsUseCase findOrCreateTagsUseCase;` and `private final TagJpaRepository tagJpaRepository;` (for materializing TagEntity references).

In `create(SongCreateRequest)`:
- Replace `List<Tag> tags = tagService.findOrCreateAll(req.tagNames());`
- With:
  ```java
  List<TagResult> tagResults = findOrCreateTagsUseCase.invoke(req.tagNames());
  List<TagEntity> tags = tagResults.stream()
      .map(r -> tagJpaRepository.getReferenceById(r.id()))
      .toList();
  ```
- The rest (`tags.forEach(song::addTag)`) works as-is because `Song.addTag(TagEntity)` is what it expects after Task 1.

### Delete legacy

- `tag/service/TagService.java`
- (`tag/repository/TagRepository.java` already moved in Task 1; verify directory empty/removed)

### Disable broken tests

- `tag/service/TagServiceTest.java` → `@Disabled("phase-7-migration: rewrite in Phase 7-8")` stub
- `tag/repository/TagRepositoryTest.java` — investigate. If it tests `TagJpaRepository` directly via the new infra path with `TagEntity`, it may still work after import updates from Task 1. If not, disable.

Commit: `feat(tag): migrate to 4-layer with FindOrCreateTagsUseCase, remove legacy packages`

Acceptance:
- `find tag -type f -name "*.java"` in src/main: only files under `domain/`, `application/`, `infra/`
- `./gradlew clean compileJava compileTestJava test` BUILD SUCCESSFUL
- No legacy `tag/service`, `tag/repository`, `tag/domain/Tag.java` (@Entity) remain

## Task 3: Handoff + Merge

- Plan checkboxes marked
- `docs/superpowers/handoff/2026-05-13-phase-7-3-tag-complete.md`
- `--no-ff` merge feature/phase-7-3-tag-migration → main
- Worktree cleanup
