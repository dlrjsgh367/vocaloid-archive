# Phase 7-4 — Like Domain Migration Plan

**Goal:** `like` 도메인 (단일 토글 엔드포인트) 을 4-layer 로 마이그레이션.

**Worktree:** `.worktrees/phase-7-4-like-migration`, branch `feature/phase-7-4-like-migration`. Base: `47848d2` (Phase 7-3 merge).

**최종 구조:**
```
like/
├── domain/Like.java                     # Pure POJO {userId, songId, likedAt}
├── application/
│   ├── ToggleLikeUseCase.java
│   ├── port/
│   │   ├── LikeRepository.java          # Command port
│   │   └── LikeQueryRepository.java     # Read port
│   └── dto/result/ToggleLikeResult.java
├── infra/persistence/
│   ├── LikeEntity.java                  # @Entity (@IdClass LikeId.java)
│   ├── LikeId.java                      # composite key holder (stays)
│   ├── LikeJpaRepository.java
│   ├── LikeRepositoryImpl.java
│   ├── LikeQueryRepositoryImpl.java
│   └── LikeEntityMapper.java
└── interfaces/
    ├── LikeController.java
    └── dto/response/LikeToggleResponse.java
```

옛 `like/{controller, service, repository, domain (@Entity), dto}` 제거.

Cross-domain: SongQueryRepository 의 `QLike` → `QLikeEntity`, static field `like → likeEntity`.

---

## Task 1: Mechanical rename `Like → LikeEntity`, move `LikeId`

- `like/domain/Like.java` → `like/infra/persistence/LikeEntity.java`
- `like/domain/LikeId.java` → `like/infra/persistence/LikeId.java` (just move, name unchanged — it's a key class, not an entity)
- `like/repository/LikeRepository.java` → `like/infra/persistence/LikeJpaRepository.java`
- Cross-domain updates: `LikeService.java`, `SongQueryRepository.java` (`QLike` → `QLikeEntity`)
- Tests: `LikeServiceTest`, `LikeControllerTest` (verify if importing entity), `SongQueryRepositoryTest` (uses QLike)
- `Like.user` field is `UserEntity` (already from Phase 7-1)
- `Like.song` field is `Song` (Phase 7-7 will handle)
- `LikeService` already uses `UserJpaRepository` (from Phase 7-1) — verify

Commit: `refactor(like): rename Like entity to LikeEntity and move to infra/persistence`

## Task 2: Pure POJO + UseCase + delete legacy + new controller

### New files

`like/domain/Like.java`:
```java
package com.vocaloidarchive.like.domain;

import java.time.LocalDateTime;

public class Like {
  private final Long userId;
  private final Long songId;
  private final LocalDateTime likedAt;

  private Like(Long userId, Long songId, LocalDateTime likedAt) {
    this.userId = userId;
    this.songId = songId;
    this.likedAt = likedAt;
  }

  public static Like newLike(Long userId, Long songId) {
    return new Like(userId, songId, null);
  }

  public static Like reconstitute(Long userId, Long songId, LocalDateTime likedAt) {
    return new Like(userId, songId, likedAt);
  }

  public Long getUserId() { return userId; }
  public Long getSongId() { return songId; }
  public LocalDateTime getLikedAt() { return likedAt; }
}
```

`like/application/dto/result/ToggleLikeResult.java`:
```java
package com.vocaloidarchive.like.application.dto.result;
public record ToggleLikeResult(boolean liked, long likeCount) {}
```

`like/application/port/LikeRepository.java`:
```java
package com.vocaloidarchive.like.application.port;
import com.vocaloidarchive.like.domain.Like;
public interface LikeRepository {
  boolean existsBy(Long userId, Long songId);
  Like save(Like like);
  void deleteBy(Long userId, Long songId);
}
```

`like/application/port/LikeQueryRepository.java`:
```java
package com.vocaloidarchive.like.application.port;
public interface LikeQueryRepository {
  long countBySongId(Long songId);
}
```

`like/infra/persistence/LikeEntityMapper.java`:
```java
package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.infra.persistence.UserEntity;

final class LikeEntityMapper {
  private LikeEntityMapper() {}

  static Like toDomain(LikeEntity e) {
    return e == null ? null : Like.reconstitute(
        e.getUser().getId(), e.getSong().getId(), e.getLikedAt());
  }

  static LikeEntity toNewEntity(UserEntity userRef, Song songRef) {
    return LikeEntity.of(userRef, songRef);
  }
}
```

`like/infra/persistence/LikeRepositoryImpl.java`:
```java
package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.like.application.port.LikeRepository;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

  private final LikeJpaRepository jpa;
  private final UserJpaRepository userJpa;
  private final SongRepository songJpa;

  @Override
  public boolean existsBy(Long userId, Long songId) {
    return jpa.existsById(new LikeId(userId, songId));
  }

  @Override
  public Like save(Like like) {
    UserEntity userRef = userJpa.getReferenceById(like.getUserId());
    Song songRef = songJpa.getReferenceById(like.getSongId());
    LikeEntity saved = jpa.save(LikeEntityMapper.toNewEntity(userRef, songRef));
    return LikeEntityMapper.toDomain(saved);
  }

  @Override
  public void deleteBy(Long userId, Long songId) {
    jpa.deleteById(new LikeId(userId, songId));
  }
}
```

`like/infra/persistence/LikeQueryRepositoryImpl.java`:
```java
package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.like.application.port.LikeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeQueryRepositoryImpl implements LikeQueryRepository {

  private final LikeJpaRepository jpa;

  @Override
  public long countBySongId(Long songId) {
    return jpa.countBySongId(songId);
  }
}
```

`like/application/ToggleLikeUseCase.java`:
```java
package com.vocaloidarchive.like.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.application.dto.result.ToggleLikeResult;
import com.vocaloidarchive.like.application.port.LikeQueryRepository;
import com.vocaloidarchive.like.application.port.LikeRepository;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleLikeUseCase {

  private final LikeRepository likeRepository;
  private final LikeQueryRepository likeQueryRepository;
  private final SongRepository songRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public ToggleLikeResult invoke(Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    boolean currentlyLiked = likeRepository.existsBy(userId, songId);
    if (currentlyLiked) {
      likeRepository.deleteBy(userId, songId);
    } else {
      likeRepository.save(Like.newLike(userId, songId));
    }
    return new ToggleLikeResult(!currentlyLiked, likeQueryRepository.countBySongId(songId));
  }
}
```

`like/interfaces/dto/response/LikeToggleResponse.java`:
```java
package com.vocaloidarchive.like.interfaces.dto.response;
import com.vocaloidarchive.like.application.dto.result.ToggleLikeResult;
public record LikeToggleResponse(boolean liked, long likeCount) {
  public static LikeToggleResponse from(ToggleLikeResult r) {
    return new LikeToggleResponse(r.liked(), r.likeCount());
  }
}
```

`like/interfaces/LikeController.java`:
```java
package com.vocaloidarchive.like.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.like.application.ToggleLikeUseCase;
import com.vocaloidarchive.like.interfaces.dto.response.LikeToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class LikeController {

  private final ToggleLikeUseCase toggleLikeUseCase;

  @PostMapping("/{id}/like")
  public ResponseEntity<ApiResponse<LikeToggleResponse>> toggle(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(
        LikeToggleResponse.from(toggleLikeUseCase.invoke(id))));
  }
}
```

### Delete

- `like/controller/LikeController.java`
- `like/service/LikeService.java`
- `like/dto/response/LikeToggleResponse.java`

### Cross-domain side-effect

`SongQueryRepository.likeCountFor / likeCountsFor` — these directly do QueryDSL `JPAExpressions.select(L.count())` etc. Doesn't need to change since QLikeEntity/LikeEntity is unchanged from QueryDSL's perspective. Reference still works.

### Critical sequence (RequestMapping collision)

Old `/api/songs/{id}/like` and new are both `@RestController`. Same approach as previous phases:
1. Create new files in application/infra/interfaces/dto/response/ (NOT new controller yet)
2. Build verify
3. Create new controller + delete old controller in same batch
4. Delete old service + old dto
5. Build verify

### Disable broken tests

- `like/service/LikeServiceTest.java` → `@Disabled` stub
- `like/controller/LikeControllerTest.java` → `@Disabled` stub

Commit: `feat(like): migrate to 4-layer with ToggleLikeUseCase, remove legacy packages`

Acceptance:
- `find like -type f` shows only: `domain/`, `application/`, `infra/persistence/`, `interfaces/`
- `./gradlew clean compileJava compileTestJava test` BUILD SUCCESSFUL
- `POST /api/songs/{id}/like` works (verified manually)

## Task 3: Handoff + Merge

- plan checkbox 마크
- handoff 문서
- `--no-ff` merge
- worktree 정리
