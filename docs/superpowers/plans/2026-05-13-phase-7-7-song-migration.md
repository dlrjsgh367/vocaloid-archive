# Phase 7-7 — Song Domain Migration Plan

**Goal:** `song` 도메인 (검색·등록·상세·삭제, QueryDSL, 다중 엔티티) 4-layer 마이그레이션. 가장 복잡한 phase.

**Worktree:** `.worktrees/phase-7-7-song-migration`, branch `feature/phase-7-7-song-migration`. Base: `1df568f` (Phase 7-6 merge).

**최종 구조:**
```
song/
├── domain/
│   ├── Song.java                              # Pure POJO scalar 필드만
│   └── Mood.java                              # enum (stays)
├── application/
│   ├── SearchSongsUseCase.java                # Read
│   ├── GetSongDetailUseCase.java              # Read+write (incrementPlayCount)
│   ├── CreateSongUseCase.java
│   ├── DeleteSongUseCase.java
│   ├── port/
│   │   ├── SongRepository.java                # Command port
│   │   └── SongQueryRepository.java           # Read port (search, detail projection)
│   └── dto/
│       ├── command/CreateSongCommand.java
│       └── result/
│           ├── SongResult.java                # list view
│           ├── SongDetailResult.java          # detail view
│           └── SongSummaryResult.java         # optional
├── infra/persistence/
│   ├── SongEntity.java
│   ├── SongCharacterEntity.java
│   ├── SongTagEntity.java
│   ├── SongCharacterId.java
│   ├── SongTagId.java
│   ├── SongJpaRepository.java
│   ├── SongQueryRepositoryImpl.java
│   ├── SongRepositoryImpl.java
│   └── SongEntityMapper.java
└── interfaces/
    ├── SongController.java
    └── dto/
        ├── request/{SongCreateRequest, SongSearchRequest, SongSort}
        └── response/{SongResponse, SongDetailResponse, UserSummary}
```

옛 `song/{controller, service, repository, domain (@Entity files), dto}` 제거 (단 `Mood` enum 은 `domain/` 에 유지).

Cross-domain 정리: Comment, Like, Playlist 의 `*RepositoryImpl` 및 UseCase 가 `song.repository.SongRepository` 와 `song.domain.Song` (@Entity) 를 참조. 이들의 import 를 `song.infra.persistence.SongJpaRepository`, `SongEntity` 로 mechanical update.

---

## Task 1: Mechanical rename (Song entities + JPA repo + cross-domain imports)

### Files to rename / move

1. `song/domain/Song.java` → `song/infra/persistence/SongEntity.java`
   - Class `Song` → `SongEntity`
   - Factory `Song.of(...)` → `SongEntity.of(...)`
   - `@OneToMany(mappedBy = "song")` fields stay as `characters`, `tags` (field names unchanged)
   - `addCharacter(CharacterEntity)`, `addTag(TagEntity)` — internal collection methods stay
   - But `SongCharacter` and `SongTag` types in fields/methods → `SongCharacterEntity`, `SongTagEntity`

2. `song/domain/SongCharacter.java` → `song/infra/persistence/SongCharacterEntity.java`
   - Field `Song song;` → `SongEntity song;`
   - Factory `SongCharacter.of(Song, CharacterEntity)` → `SongCharacterEntity.of(SongEntity, CharacterEntity)`

3. `song/domain/SongCharacterId.java` → `song/infra/persistence/SongCharacterId.java`

4. `song/domain/SongTag.java` → `song/infra/persistence/SongTagEntity.java`
   - Similar — fields/factory updated

5. `song/domain/SongTagId.java` → `song/infra/persistence/SongTagId.java`

6. `song/domain/Mood.java` — STAYS at `song/domain/` (enum, not @Entity)

7. `song/repository/SongRepository.java` → `song/infra/persistence/SongJpaRepository.java`
   - `JpaRepository<Song, Long>` → `JpaRepository<SongEntity, Long>`
   - `Optional<Song>` → `Optional<SongEntity>`
   - **JPQL updates:**
     - `"update Song s set s.playCount = s.playCount + 1 where s.id = :id"` → `"update SongEntity s set s.playCount = s.playCount + 1 where s.id = :id"`
     - `"select distinct s from Song s join fetch s.registeredBy left join fetch s.characters sc left join fetch sc.character where s.id = :id"` → `... from SongEntity s ...`
     - `"select distinct s from Song s left join fetch s.tags st left join fetch st.tag where s.id = :id"` → `... from SongEntity s ...`

8. `song/repository/SongQueryRepository.java` → `song/infra/persistence/SongQueryRepositoryImpl.java`
   - Rename to `Impl` to align with port-adapter pattern (will be wired to a port interface in Task 2)
   - For NOW (Task 1), keep as standalone `@Repository` and update QueryDSL references:
     - `QSong.song` → `QSongEntity.songEntity`
     - `QSongCharacter.songCharacter` → `QSongCharacterEntity.songCharacterEntity`
     - `QSongTag.songTag` → `QSongTagEntity.songTagEntity`
     - Type `Song` → `SongEntity` in method signatures and variables
   - **Note:** Task 2 will convert this into a port-adapter; in Task 1 just rename file + update internals
   - **Important:** keep the class type signatures (Page<Song> etc.) updated to Page<SongEntity>. Callers (SongService still legacy) handle the type change.

### Cross-domain import updates (Comment, Like, Playlist)

In each of these files, update `import com.vocaloidarchive.song.domain.Song` → `import com.vocaloidarchive.song.infra.persistence.SongEntity` and field/method types `Song` → `SongEntity`. Update `import com.vocaloidarchive.song.repository.SongRepository` → `import com.vocaloidarchive.song.infra.persistence.SongJpaRepository` and field types.

Files (12 main + tests):
- `comment/application/CreateCommentUseCase.java`, `ListCommentsUseCase.java`
- `comment/infra/persistence/CommentEntity.java` (`@ManyToOne Song` → `SongEntity`)
- `comment/infra/persistence/CommentRepositoryImpl.java`
- `like/application/ToggleLikeUseCase.java`
- `like/infra/persistence/LikeEntity.java` (`@ManyToOne Song` → `SongEntity`)
- `like/infra/persistence/LikeRepositoryImpl.java`
- `playlist/application/AddSongToPlaylistUseCase.java`
- `playlist/infra/persistence/PlaylistSongEntity.java` (`@ManyToOne Song` → `SongEntity`)
- `playlist/infra/persistence/PlaylistSongRepositoryImpl.java`
- Also `song/service/SongService.java`, `song/dto/response/{SongResponse, SongDetailResponse}.java` (will be deleted in Task 2 but must compile through Task 1)

### Tests

`SongServiceTest`, `SongControllerTest`, `SongRepositoryTest`, `SongQueryRepositoryTest`, `CommentRepositoryTest`, `LikeServiceTest` (already disabled), `PlaylistRepositoryTest`, `PlaylistSongRepositoryTest` — all need import + type updates.

### Constraints

- DO NOT introduce Pure POJO `song.domain.Song` yet (Task 2)
- DO NOT rename method-local variables (`Song song` → `SongEntity song`, NOT `songEntity`)
- DO NOT change JPQL field names (`s.title`, `s.registeredBy`, `s.characters`, etc. stay)
- DO NOT delete service/controller/dto yet (Task 2)
- Move `Mood` enum stays at `song/domain/Mood.java`

### Acceptance

1. `grep -rn "com.vocaloidarchive.song.domain.Song\b"` → 0 matches in main (Mood enum imports OK; test package decls OK)
2. `grep -rn "com.vocaloidarchive.song.repository"` → 0 in source
3. `grep -rn "QSong\b\|QSongCharacter\b\|QSongTag\b"` → 0 matches; all `*Entity` variants now
4. `./gradlew clean compileJava compileTestJava test` BUILD SUCCESSFUL
5. ONE commit:
```
refactor(song): rename Song/SongCharacter/SongTag entities to *Entity and move to infra/persistence

Mechanical rename + JPQL/QueryDSL entity name updates. Cross-domain imports updated.
No behavior change.
```

## Task 2: Pure POJO + application stack + new controller + delete legacy

### Pure POJO

`song/domain/Song.java`:
```java
package com.vocaloidarchive.song.domain;
import java.time.LocalDateTime;
public class Song {
  private final Long id;
  private final Long registeredById;
  private final String title;
  private final String youtubeUrl;
  private final String niconicoUrl;
  private final String thumbnailUrl;
  private final Integer bpm;
  private final Mood mood;
  private final Integer playCount;
  private final LocalDateTime createdAt;

  private Song(Long id, Long registeredById, String title, String youtubeUrl, String niconicoUrl,
               String thumbnailUrl, Integer bpm, Mood mood, Integer playCount, LocalDateTime createdAt) {
    this.id = id; this.registeredById = registeredById; this.title = title;
    this.youtubeUrl = youtubeUrl; this.niconicoUrl = niconicoUrl;
    this.thumbnailUrl = thumbnailUrl; this.bpm = bpm; this.mood = mood;
    this.playCount = playCount; this.createdAt = createdAt;
  }
  public static Song newSong(Long registeredById, String title, String youtubeUrl, String niconicoUrl,
                              String thumbnailUrl, Integer bpm, Mood mood) {
    return new Song(null, registeredById, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, 0, null);
  }
  public static Song reconstitute(Long id, Long registeredById, String title, String youtubeUrl, String niconicoUrl,
                                   String thumbnailUrl, Integer bpm, Mood mood, Integer playCount, LocalDateTime createdAt) {
    return new Song(id, registeredById, title, youtubeUrl, niconicoUrl,
        thumbnailUrl, bpm, mood, playCount, createdAt);
  }
  public boolean isRegisteredBy(Long userId) {
    return registeredById != null && registeredById.equals(userId);
  }
  // 10 getters
  public Long getId() { return id; }
  public Long getRegisteredById() { return registeredById; }
  public String getTitle() { return title; }
  public String getYoutubeUrl() { return youtubeUrl; }
  public String getNiconicoUrl() { return niconicoUrl; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public Integer getBpm() { return bpm; }
  public Mood getMood() { return mood; }
  public Integer getPlayCount() { return playCount; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
```

### Command

`song/application/dto/command/CreateSongCommand.java`:
```java
package com.vocaloidarchive.song.application.dto.command;
import com.vocaloidarchive.song.domain.Mood;
import java.util.List;
public record CreateSongCommand(
    Long userId, String title, String youtubeUrl, String niconicoUrl,
    Integer bpm, Mood mood, List<Long> characterIds, List<String> tagNames) {}
```

### Results

`song/application/dto/result/SongResult.java`:
```java
package com.vocaloidarchive.song.application.dto.result;
import com.vocaloidarchive.song.domain.Mood;
import java.time.LocalDateTime;
import java.util.List;
public record SongResult(
    Long id, String title, String thumbnailUrl, Mood mood, Integer playCount,
    Long likeCount, Owner registeredBy, List<CharacterRef> characters, List<String> tags,
    LocalDateTime createdAt) {
  public record Owner(Long id, String username) {}
  public record CharacterRef(Long id, String name, String colorHex, String imageUrl) {}
}
```

`song/application/dto/result/SongDetailResult.java`:
```java
package com.vocaloidarchive.song.application.dto.result;
import com.vocaloidarchive.song.domain.Mood;
import java.time.LocalDateTime;
import java.util.List;
public record SongDetailResult(
    Long id, String title, String youtubeUrl, String niconicoUrl, String thumbnailUrl,
    Integer bpm, Mood mood, Integer playCount, Long likeCount,
    SongResult.Owner registeredBy, List<SongResult.CharacterRef> characters,
    List<String> tags, LocalDateTime createdAt) {}
```

### Ports

`song/application/port/SongRepository.java`:
```java
package com.vocaloidarchive.song.application.port;
import com.vocaloidarchive.song.domain.Song;
import java.util.Optional;
public interface SongRepository {
  Song save(Song song, java.util.List<Long> characterIds, java.util.List<Long> tagIds);
  Optional<Song> findById(Long id);
  boolean existsById(Long id);
  int incrementPlayCount(Long id);
  void deleteById(Long id);
}
```

`song/application/port/SongQueryRepository.java`:
```java
package com.vocaloidarchive.song.application.port;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.song.application.dto.result.SongDetailResult;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface SongQueryRepository {
  PageResponse<SongResult> search(
      String keyword, com.vocaloidarchive.song.domain.Mood mood,
      Long characterId, Long tagId,
      com.vocaloidarchive.song.application.SongSortKey sort, Pageable pageable);
  Optional<SongDetailResult> findDetailById(Long id);
}
```

`song/application/SongSortKey.java` (enum, replaces dto/request/SongSort.java):
```java
package com.vocaloidarchive.song.application;
public enum SongSortKey { LATEST, POPULAR, PLAYED }
```

### Infra impl

`SongRepositoryImpl`, `SongQueryRepositoryImpl`, `SongEntityMapper` — bridge new ports to existing JPA + QueryDSL. The QueryDSL search query (currently in `SongQueryRepository.java` legacy) moves into `SongQueryRepositoryImpl.search(...)` and returns `PageResponse<SongResult>` (with all the joins to compose `Owner`, `CharacterRef`, etc.).

Critical: `SongQueryRepositoryImpl.search` must preserve current behavior:
- keyword search on title / character.name / tag.name
- mood filter, characterId filter, tagId filter
- sort POPULAR (likeCount desc) / PLAYED (playCount desc) / LATEST (createdAt desc)
- distinct + offset + limit
- like count subquery via QLikeEntity

`SongRepositoryImpl.save(Song, characterIds, tagIds)` — convert domain Song to SongEntity, load CharacterEntity by IDs, load TagEntity by IDs (or use `getReferenceById`), addCharacter/addTag, save.

### UseCases

`SearchSongsUseCase.invoke(keyword, mood, charId, tagId, sort, pageable): PageResponse<SongResult>` — delegates to SongQueryRepository.search

`GetSongDetailUseCase.invoke(id): SongDetailResult`:
```java
@Transactional
public SongDetailResult invoke(Long id) {
  int affected = songRepository.incrementPlayCount(id);
  if (affected == 0) throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
  return songQueryRepository.findDetailById(id)
      .orElseThrow(() -> new BusinessException(ErrorCode.SONG_NOT_FOUND));
}
```

`CreateSongUseCase`:
- validate character IDs exist
- find/create tags via `FindOrCreateTagsUseCase` (from Phase 7-3)
- compose Song domain (using `YoutubeUtil.extractThumbnailUrl`)
- `songRepository.save(song, characterIds, tagIds)` returns saved Song
- compose SongResult with empty initial likeCount=0 and character/tag info — fetch through QueryRepo or compose manually
- return SongResult

`DeleteSongUseCase`:
- security check via SecurityUtil
- find song, check `isRegisteredBy(currentUserId)`, throw FORBIDDEN otherwise
- delete

### Interfaces

`SongController` uses 4 UseCases. Pageable handling unchanged. `SongSearchRequest`, `SongCreateRequest`, `SongSort` move to `interfaces/dto/request/`. `SongResponse`, `SongDetailResponse`, `UserSummary` move to `interfaces/dto/response/`. Response DTOs map from Result DTOs.

`SongSort` request enum (`LATEST` / `POPULAR` / `PLAYED`) maps to `SongSortKey` 1:1.

### Delete

- `song/controller/`, `song/service/`, `song/repository/`, `song/dto/` 전체
- 옛 SongQueryRepository (legacy QueryDSL) 삭제 (Impl 로 대체됨)

### Cross-domain port migration

After Song migration done, update other domains to use the new `song.application.port.SongRepository`:
- `Comment/Like/Playlist` `*RepositoryImpl` and UseCases: replace `SongJpaRepository songJpa` field with `SongRepository songPort` (port interface)
- For `existsById(songId)`, `getReferenceById(songId)` patterns — port interface should provide equivalent. But `getReferenceById` returns SongEntity which is needed for ManyToOne assignment...

**Compromise (recommended):** other domain `*RepositoryImpl` continue to use legacy `SongJpaRepository` (now at `song.infra.persistence.SongJpaRepository`) — same-name import update only. Their UseCases use new port `SongRepository.existsById`. This is the minimal change preserving cross-domain port purity at the UseCase layer.

Specifically:
- `ToggleLikeUseCase`, `CreateCommentUseCase`, `ListCommentsUseCase`, `AddSongToPlaylistUseCase`: replace `SongRepository` (legacy) with new port `SongRepository`
- `LikeRepositoryImpl`, `CommentRepositoryImpl`, `PlaylistSongRepositoryImpl`: continue to use `SongJpaRepository` for `getReferenceById` (cross-domain infra coupling — Modular Monolith later)

### Disable broken tests

- `SongServiceTest`, `SongControllerTest`, `SongRepositoryTest`, `SongQueryRepositoryTest` — `@Disabled` stubs (rewrite Phase 7-8)

Commit: `feat(song): migrate to 4-layer with 4 UseCases, port-based cross-domain access`

## Task 3: Handoff + Merge
