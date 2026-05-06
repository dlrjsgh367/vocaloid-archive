# Phase 5: Playlist Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Playlist domain — CRUD + song add/remove — following Phase 3–4 patterns exactly.

**Architecture:** `Playlist` (single PK) + `PlaylistSong` (@IdClass composite PK) entities. `PlaylistService` handles access control (private playlists: only owner). No new Flyway migration needed (V1 schema already has `playlists` + `playlist_songs` tables).

**Tech Stack:** Spring Boot 3.2, JPA + Hibernate 6, Spring Data JPA, JUnit 5 + Mockito BDD, Testcontainers MySQL (@DataJpaTest), @WebMvcTest + Spring Security test

---

## Context (read before coding)

**Main design spec:** `docs/superpowers/specs/2026-05-03-vocaloid-archive-design.md` §5.2 Playlist endpoints, §4.3 JPA mapping decisions.

**API endpoints:**
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/api/playlists` | O | My playlists only |
| POST | `/api/playlists` | O | `{title, isPublic}` |
| GET | `/api/playlists/{id}` | △ | private→owner only, public→anyone |
| DELETE | `/api/playlists/{id}` | O | owner only |
| POST | `/api/playlists/{id}/songs` | O | `{songId}`, orderIndex = max+1 |
| DELETE | `/api/playlists/{id}/songs/{songId}` | O | owner only |

**SecurityConfig already covers these** — no changes needed:
- `GET /api/playlists/*` → `permitAll()` (line 52)
- Everything else → `anyRequest().authenticated()`

**SecurityUtil.getCurrentUserId()** throws `BusinessException(ErrorCode.INVALID_TOKEN)` if called by an anonymous user. In `PlaylistService.getDetail`, catch this and rethrow as `FORBIDDEN` for private-playlist-by-anonymous-user case.

**Established patterns (follow exactly):**
- `Like.java` / `LikeId.java` → pattern for `PlaylistSong` / `PlaylistSongId`
- `CommentService.java` → pattern for service transaction + ownership check
- `CommentController.java` → pattern for controller (page-size cap, ResponseEntity)
- `SongRepositoryTest.java` → `@DataJpaTest @AutoConfigureTestDatabase(replace=NONE) @Import(AuditingConfig.class)` + `extends AbstractMysqlContainerTest`
- `SongServiceTest.java` → Mockito BDD (`@ExtendWith(MockitoExtension.class)`)
- `SongControllerTest.java` → `@WebMvcTest @Import({SecurityConfig, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtAccessDeniedHandler})`

**Key package:**
```
backend/src/main/java/com/vocaloidarchive/playlist/
backend/src/test/java/com/vocaloidarchive/playlist/
```

---

## File Map

| File | Create/Modify |
|------|--------------|
| `playlist/domain/Playlist.java` | Create |
| `playlist/domain/PlaylistSong.java` | Create |
| `playlist/domain/PlaylistSongId.java` | Create |
| `playlist/repository/PlaylistRepository.java` | Create |
| `playlist/repository/PlaylistSongRepository.java` | Create |
| `playlist/dto/request/PlaylistCreateRequest.java` | Create |
| `playlist/dto/request/PlaylistSongAddRequest.java` | Create |
| `playlist/dto/response/PlaylistResponse.java` | Create |
| `playlist/dto/response/PlaylistDetailResponse.java` | Create |
| `playlist/service/PlaylistService.java` | Create |
| `playlist/controller/PlaylistController.java` | Create |
| `(test) playlist/repository/PlaylistRepositoryTest.java` | Create |
| `(test) playlist/repository/PlaylistSongRepositoryTest.java` | Create |
| `(test) playlist/service/PlaylistServiceTest.java` | Create |
| `(test) playlist/controller/PlaylistControllerTest.java` | Create |

---

## Task 1: Playlist + PlaylistSong Domain + Repositories + @DataJpaTest

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/domain/Playlist.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/domain/PlaylistSong.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/domain/PlaylistSongId.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/repository/PlaylistRepository.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/repository/PlaylistSongRepository.java`
- Create: `backend/src/test/java/com/vocaloidarchive/playlist/repository/PlaylistRepositoryTest.java`
- Create: `backend/src/test/java/com/vocaloidarchive/playlist/repository/PlaylistSongRepositoryTest.java`

- [ ] **Step 1: Write `PlaylistRepositoryTest` — failing first**

```java
package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class PlaylistRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired PlaylistRepository playlistRepository;
  @Autowired UserRepository userRepository;
  @PersistenceContext EntityManager em;

  @Test
  @DisplayName("findAllByUserId: 본인 플레이리스트만 반환")
  void findAllByUserId_returnsOwnerPlaylistsOnly() {
    User alice = userRepository.save(User.of("alice", "alice@a.com", "hash"));
    User bob   = userRepository.save(User.of("bob",   "bob@a.com",   "hash"));
    playlistRepository.save(Playlist.of(alice, "A-1", true));
    playlistRepository.save(Playlist.of(alice, "A-2", false));
    playlistRepository.save(Playlist.of(bob,   "B-1", true));
    em.flush();
    em.clear();

    List<Playlist> result = playlistRepository.findAllByUserId(alice.getId());

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(p -> p.getUser().getUsername().equals("alice"));
  }

  @Test
  @DisplayName("findById: 저장한 playlist 조회")
  void findById_returnsSavedPlaylist() {
    User u = userRepository.save(User.of("carol", "carol@a.com", "hash"));
    Playlist saved = playlistRepository.save(Playlist.of(u, "My List", false));
    em.flush();
    em.clear();

    Playlist found = playlistRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getTitle()).isEqualTo("My List");
    assertThat(found.isPublic()).isFalse();
  }
}
```

- [ ] **Step 2: Run test — expect compilation failure (classes don't exist yet)**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.repository.PlaylistRepositoryTest" 2>&1 | tail -20
```

Expected: compilation error — `Playlist`, `PlaylistRepository` not found.

- [ ] **Step 3: Create `Playlist.java`**

```java
package com.vocaloidarchive.playlist.domain;

import com.vocaloidarchive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "playlists")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public static Playlist of(User user, String title, boolean isPublic) {
    Playlist p = new Playlist();
    p.user = user;
    p.title = title;
    p.isPublic = isPublic;
    return p;
  }
}
```

- [ ] **Step 4: Create `PlaylistSongId.java`** (mirrors `LikeId.java` exactly)

```java
package com.vocaloidarchive.playlist.domain;

import java.io.Serializable;
import java.util.Objects;

public class PlaylistSongId implements Serializable {

  private Long playlist;
  private Long song;

  public PlaylistSongId() {}

  public PlaylistSongId(Long playlist, Long song) {
    this.playlist = playlist;
    this.song = song;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PlaylistSongId that)) return false;
    return Objects.equals(playlist, that.playlist) && Objects.equals(song, that.song);
  }

  @Override
  public int hashCode() {
    return Objects.hash(playlist, song);
  }
}
```

- [ ] **Step 5: Create `PlaylistSong.java`**

```java
package com.vocaloidarchive.playlist.domain;

import com.vocaloidarchive.song.domain.Song;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_songs")
@IdClass(PlaylistSongId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSong {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  public static PlaylistSong of(Playlist playlist, Song song, int orderIndex) {
    PlaylistSong ps = new PlaylistSong();
    ps.playlist = playlist;
    ps.song = song;
    ps.orderIndex = orderIndex;
    return ps;
  }
}
```

- [ ] **Step 6: Create `PlaylistRepository.java`**

```java
package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.playlist.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

  @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
  List<Playlist> findAllByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 7: Run `PlaylistRepositoryTest` — expect 2 green tests**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.repository.PlaylistRepositoryTest"
```

Expected: BUILD SUCCESSFUL, 2 tests passing.

- [ ] **Step 8: Write `PlaylistSongRepositoryTest` — failing first**

```java
package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class PlaylistSongRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired PlaylistRepository playlistRepository;
  @Autowired PlaylistSongRepository playlistSongRepository;
  @Autowired SongRepository songRepository;
  @Autowired UserRepository userRepository;
  @PersistenceContext EntityManager em;

  private Playlist playlist;
  private Song song1;
  private Song song2;

  @BeforeEach
  void setUp() {
    User u = userRepository.save(User.of("test", "t@a.com", "hash"));
    playlist = playlistRepository.save(Playlist.of(u, "My List", true));
    song1 = songRepository.save(Song.of(u, "Song 1", null, null, null, null, Mood.BRIGHT));
    song2 = songRepository.save(Song.of(u, "Song 2", null, null, null, null, Mood.CALM));
    em.flush();
  }

  @Test
  @DisplayName("findWithSongByPlaylistId: order_index 오름차순으로 반환")
  void findWithSongByPlaylistId_orderedAsc() {
    playlistSongRepository.save(PlaylistSong.of(playlist, song2, 2));
    playlistSongRepository.save(PlaylistSong.of(playlist, song1, 1));
    em.flush();
    em.clear();

    List<PlaylistSong> result = playlistSongRepository.findWithSongByPlaylistId(playlist.getId());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getSong().getTitle()).isEqualTo("Song 1");
    assertThat(result.get(1).getSong().getTitle()).isEqualTo("Song 2");
  }

  @Test
  @DisplayName("findMaxOrderIndexByPlaylistId: 비어있으면 0, 있으면 최대값")
  void findMaxOrderIndexByPlaylistId() {
    assertThat(playlistSongRepository.findMaxOrderIndexByPlaylistId(playlist.getId())).isEqualTo(0);

    playlistSongRepository.save(PlaylistSong.of(playlist, song1, 1));
    playlistSongRepository.save(PlaylistSong.of(playlist, song2, 3));
    em.flush();

    assertThat(playlistSongRepository.findMaxOrderIndexByPlaylistId(playlist.getId())).isEqualTo(3);
  }

  @Test
  @DisplayName("existsByPlaylistIdAndSongId: 존재 여부 확인")
  void existsByPlaylistIdAndSongId() {
    assertThat(playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), song1.getId())).isFalse();
    playlistSongRepository.save(PlaylistSong.of(playlist, song1, 1));
    em.flush();
    assertThat(playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), song1.getId())).isTrue();
  }

  @Test
  @DisplayName("deleteByPlaylistIdAndSongId: 해당 row 삭제")
  void deleteByPlaylistIdAndSongId_removesRow() {
    playlistSongRepository.save(PlaylistSong.of(playlist, song1, 1));
    em.flush();

    playlistSongRepository.deleteByPlaylistIdAndSongId(playlist.getId(), song1.getId());
    em.flush();
    em.clear();

    assertThat(playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), song1.getId())).isFalse();
  }

  @Test
  @DisplayName("countByPlaylistId: 곡 수 반환")
  void countByPlaylistId() {
    assertThat(playlistSongRepository.countByPlaylistId(playlist.getId())).isEqualTo(0L);
    playlistSongRepository.save(PlaylistSong.of(playlist, song1, 1));
    playlistSongRepository.save(PlaylistSong.of(playlist, song2, 2));
    em.flush();
    assertThat(playlistSongRepository.countByPlaylistId(playlist.getId())).isEqualTo(2L);
  }
}
```

- [ ] **Step 9: Create `PlaylistSongRepository.java`**

```java
package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.playlist.domain.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {

  @Query("SELECT ps FROM PlaylistSong ps JOIN FETCH ps.song WHERE ps.playlist.id = :playlistId ORDER BY ps.orderIndex ASC")
  List<PlaylistSong> findWithSongByPlaylistId(@Param("playlistId") Long playlistId);

  @Query("SELECT COALESCE(MAX(ps.orderIndex), 0) FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId")
  int findMaxOrderIndexByPlaylistId(@Param("playlistId") Long playlistId);

  boolean existsByPlaylistIdAndSongId(Long playlistId, Long songId);

  @Modifying
  @Query("DELETE FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId AND ps.song.id = :songId")
  void deleteByPlaylistIdAndSongId(@Param("playlistId") Long playlistId, @Param("songId") Long songId);

  long countByPlaylistId(Long playlistId);
}
```

Note: `existsByPlaylistIdAndSongId` and `countByPlaylistId` use Spring Data property traversal — `playlistId` → `playlist.id`, `songId` → `song.id`.

- [ ] **Step 10: Run `PlaylistSongRepositoryTest` — expect 5 green tests**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.repository.PlaylistSongRepositoryTest"
```

Expected: BUILD SUCCESSFUL, 5 tests passing.

- [ ] **Step 11: Run full test suite to confirm no regressions**

```
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 12: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/playlist/domain/ \
        backend/src/main/java/com/vocaloidarchive/playlist/repository/ \
        backend/src/test/java/com/vocaloidarchive/playlist/repository/
git commit -m "feat(playlist): add Playlist/PlaylistSong entities, repositories, @DataJpaTest"
```

---

## Task 2: DTOs + PlaylistService + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/dto/request/PlaylistCreateRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/dto/request/PlaylistSongAddRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/dto/response/PlaylistResponse.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/dto/response/PlaylistDetailResponse.java`
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/service/PlaylistService.java`
- Create: `backend/src/test/java/com/vocaloidarchive/playlist/service/PlaylistServiceTest.java`

- [ ] **Step 1: Write `PlaylistServiceTest` — failing first**

```java
package com.vocaloidarchive.playlist.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.repository.PlaylistRepository;
import com.vocaloidarchive.playlist.repository.PlaylistSongRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

  @Mock PlaylistRepository playlistRepository;
  @Mock PlaylistSongRepository playlistSongRepository;
  @Mock SongRepository songRepository;
  @Mock UserRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks PlaylistService playlistService;

  private User owner() {
    User u = User.of("alice", "alice@a.com", "hash");
    ReflectionTestUtils.setField(u, "id", 1L);
    return u;
  }

  private Playlist playlist(User u, boolean isPublic) {
    Playlist p = Playlist.of(u, "My List", isPublic);
    ReflectionTestUtils.setField(p, "id", 10L);
    return p;
  }

  // ── getMyPlaylists ────────────────────────────────────────────────────────

  @Test
  @DisplayName("getMyPlaylists: 본인 플레이리스트 목록 반환")
  void getMyPlaylists_returnsList() {
    User u = owner();
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findAllByUserId(1L)).willReturn(List.of(
        playlist(u, true), playlist(u, false)));
    given(playlistSongRepository.countByPlaylistId(10L)).willReturn(3L);

    List<PlaylistResponse> result = playlistService.getMyPlaylists();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).songCount()).isEqualTo(3);
  }

  // ── getDetail ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("getDetail: public playlist → 누구나 접근")
  void getDetail_public_ok() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(playlistSongRepository.findWithSongByPlaylistId(10L)).willReturn(List.of());

    PlaylistDetailResponse result = playlistService.getDetail(10L);

    assertThat(result.title()).isEqualTo("My List");
    assertThat(result.isPublic()).isTrue();
  }

  @Test
  @DisplayName("getDetail: private playlist → 본인 접근")
  void getDetail_private_ownerAccess() {
    User u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistSongRepository.findWithSongByPlaylistId(10L)).willReturn(List.of());

    PlaylistDetailResponse result = playlistService.getDetail(10L);

    assertThat(result.isPublic()).isFalse();
  }

  @Test
  @DisplayName("getDetail: private playlist → 타인 → FORBIDDEN")
  void getDetail_private_otherUser_forbidden() {
    User u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willReturn(2L);

    assertThatThrownBy(() -> playlistService.getDetail(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("getDetail: private playlist → 미인증 → FORBIDDEN")
  void getDetail_private_anonymous_forbidden() {
    User u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

    assertThatThrownBy(() -> playlistService.getDetail(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("getDetail: 없는 id → PLAYLIST_NOT_FOUND")
  void getDetail_notFound() {
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.getDetail(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create: 저장 후 PlaylistResponse 반환")
  void create_ok() {
    User u = owner();
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(userRepository.getReferenceById(1L)).willReturn(u);
    given(playlistRepository.save(any(Playlist.class))).willAnswer(inv -> inv.getArgument(0));
    given(playlistSongRepository.countByPlaylistId(any())).willReturn(0L);

    PlaylistResponse result = playlistService.create(new PlaylistCreateRequest("New List", true));

    assertThat(result.title()).isEqualTo("New List");
    assertThat(result.isPublic()).isTrue();
    then(playlistRepository).should().save(any(Playlist.class));
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete: 본인 → repo.delete 호출")
  void delete_owner_ok() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    playlistService.delete(10L);

    then(playlistRepository).should().delete(p);
  }

  @Test
  @DisplayName("delete: 타인 → FORBIDDEN")
  void delete_forbidden() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.delete(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("delete: 없는 id → PLAYLIST_NOT_FOUND")
  void delete_notFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.delete(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  // ── addSong ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("addSong: 새 곡 추가, orderIndex = max+1")
  void addSong_newSong_savedWithOrder() {
    User u = owner();
    Playlist p = playlist(u, true);
    Song song = Song.of(u, "T", null, null, null, null, Mood.BRIGHT);
    ReflectionTestUtils.setField(song, "id", 5L);

    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(5L)).willReturn(true);
    given(playlistSongRepository.existsByPlaylistIdAndSongId(10L, 5L)).willReturn(false);
    given(playlistSongRepository.findMaxOrderIndexByPlaylistId(10L)).willReturn(2);
    given(songRepository.getReferenceById(5L)).willReturn(song);

    playlistService.addSong(10L, new PlaylistSongAddRequest(5L));

    then(playlistSongRepository).should().save(any(PlaylistSong.class));
  }

  @Test
  @DisplayName("addSong: 이미 있는 곡 → skip (save 미호출)")
  void addSong_duplicate_skipped() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(5L)).willReturn(true);
    given(playlistSongRepository.existsByPlaylistIdAndSongId(10L, 5L)).willReturn(true);

    playlistService.addSong(10L, new PlaylistSongAddRequest(5L));

    then(playlistSongRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("addSong: playlist 없음 → PLAYLIST_NOT_FOUND")
  void addSong_playlistNotFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.addSong(99L, new PlaylistSongAddRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("addSong: 타인 → FORBIDDEN")
  void addSong_forbidden() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.addSong(10L, new PlaylistSongAddRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("addSong: 곡 없음 → SONG_NOT_FOUND")
  void addSong_songNotFound() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(99L)).willReturn(false);

    assertThatThrownBy(() -> playlistService.addSong(10L, new PlaylistSongAddRequest(99L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  // ── removeSong ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("removeSong: 본인 → deleteByPlaylistIdAndSongId 호출")
  void removeSong_owner_ok() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    playlistService.removeSong(10L, 5L);

    then(playlistSongRepository).should().deleteByPlaylistIdAndSongId(10L, 5L);
  }

  @Test
  @DisplayName("removeSong: 타인 → FORBIDDEN")
  void removeSong_forbidden() {
    User u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.removeSong(10L, 5L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("removeSong: playlist 없음 → PLAYLIST_NOT_FOUND")
  void removeSong_notFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.removeSong(99L, 5L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }
}
```

- [ ] **Step 2: Run test — expect compilation failure**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.service.PlaylistServiceTest" 2>&1 | tail -20
```

Expected: compilation error — DTOs and service don't exist yet.

- [ ] **Step 3: Create DTOs**

`PlaylistCreateRequest.java`:
```java
package com.vocaloidarchive.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank @Size(max = 200) String title,
    boolean isPublic
) {}
```

`PlaylistSongAddRequest.java`:
```java
package com.vocaloidarchive.playlist.dto.request;

import jakarta.validation.constraints.NotNull;

public record PlaylistSongAddRequest(@NotNull Long songId) {}
```

`PlaylistResponse.java`:
```java
package com.vocaloidarchive.playlist.dto.response;

import com.vocaloidarchive.playlist.domain.Playlist;

import java.time.LocalDateTime;

public record PlaylistResponse(
    Long id,
    String title,
    boolean isPublic,
    String ownerUsername,
    long songCount,
    LocalDateTime createdAt
) {
  public static PlaylistResponse from(Playlist p, long songCount) {
    return new PlaylistResponse(p.getId(), p.getTitle(), p.isPublic(),
        p.getUser().getUsername(), songCount, p.getCreatedAt());
  }
}
```

`PlaylistDetailResponse.java`:
```java
package com.vocaloidarchive.playlist.dto.response;

import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDetailResponse(
    Long id,
    String title,
    boolean isPublic,
    String ownerUsername,
    List<SongItem> songs,
    LocalDateTime createdAt
) {
  public record SongItem(Long songId, String title, String thumbnailUrl, int orderIndex) {}

  public static PlaylistDetailResponse from(Playlist p, List<PlaylistSong> songs) {
    List<SongItem> items = songs.stream()
        .map(ps -> new SongItem(ps.getSong().getId(), ps.getSong().getTitle(),
            ps.getSong().getThumbnailUrl(), ps.getOrderIndex()))
        .toList();
    return new PlaylistDetailResponse(p.getId(), p.getTitle(), p.isPublic(),
        p.getUser().getUsername(), items, p.getCreatedAt());
  }
}
```

- [ ] **Step 4: Create `PlaylistService.java`**

```java
package com.vocaloidarchive.playlist.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.repository.PlaylistRepository;
import com.vocaloidarchive.playlist.repository.PlaylistSongRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistSongRepository playlistSongRepository;
  private final SongRepository songRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  public List<PlaylistResponse> getMyPlaylists() {
    Long userId = securityUtil.getCurrentUserId();
    return playlistRepository.findAllByUserId(userId).stream()
        .map(p -> PlaylistResponse.from(p, playlistSongRepository.countByPlaylistId(p.getId())))
        .toList();
  }

  public PlaylistDetailResponse getDetail(Long id) {
    Playlist playlist = playlistRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isPublic()) {
      Long currentUserId;
      try {
        currentUserId = securityUtil.getCurrentUserId();
      } catch (BusinessException e) {
        // anonymous user (getCurrentUserId throws INVALID_TOKEN)
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      if (!playlist.getUser().getId().equals(currentUserId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
    }
    List<PlaylistSong> songs = playlistSongRepository.findWithSongByPlaylistId(id);
    return PlaylistDetailResponse.from(playlist, songs);
  }

  @Transactional
  public PlaylistResponse create(PlaylistCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    User user = userRepository.getReferenceById(userId);
    Playlist saved = playlistRepository.save(Playlist.of(user, req.title(), req.isPublic()));
    return PlaylistResponse.from(saved, 0);
  }

  @Transactional
  public void delete(Long id) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    playlistRepository.delete(playlist);
  }

  @Transactional
  public void addSong(Long playlistId, PlaylistSongAddRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!songRepository.existsById(req.songId())) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    if (!playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, req.songId())) {
      int nextOrder = playlistSongRepository.findMaxOrderIndexByPlaylistId(playlistId) + 1;
      Song song = songRepository.getReferenceById(req.songId());
      playlistSongRepository.save(PlaylistSong.of(playlist, song, nextOrder));
    }
  }

  @Transactional
  public void removeSong(Long playlistId, Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
  }
}
```

- [ ] **Step 5: Run `PlaylistServiceTest` — expect all tests green**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.service.PlaylistServiceTest"
```

Expected: BUILD SUCCESSFUL, 16 tests passing.

- [ ] **Step 6: Run full test suite — no regressions**

```
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/playlist/dto/ \
        backend/src/main/java/com/vocaloidarchive/playlist/service/ \
        backend/src/test/java/com/vocaloidarchive/playlist/service/
git commit -m "feat(playlist): add DTOs, PlaylistService with ownership control and unit tests"
```

---

## Task 3: PlaylistController + WebMvcTest

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/playlist/controller/PlaylistController.java`
- Create: `backend/src/test/java/com/vocaloidarchive/playlist/controller/PlaylistControllerTest.java`

**Note:** `SecurityConfig` requires **no changes** — `GET /api/playlists/*` is already `permitAll()` (line 52) and all write endpoints are covered by `anyRequest().authenticated()`.

- [ ] **Step 1: Write `PlaylistControllerTest` — failing first**

```java
package com.vocaloidarchive.playlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaylistController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class PlaylistControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean PlaylistService playlistService;

  private PlaylistResponse sampleList() {
    return new PlaylistResponse(1L, "My List", true, "alice", 3L, LocalDateTime.now());
  }

  private PlaylistDetailResponse sampleDetail() {
    return new PlaylistDetailResponse(1L, "My List", true, "alice", List.of(), LocalDateTime.now());
  }

  // ── GET /api/playlists ────────────────────────────────────────────────────

  @Test
  @WithMockUser
  void getMyPlaylists_authenticated_ok() throws Exception {
    given(playlistService.getMyPlaylists()).willReturn(List.of(sampleList()));

    mockMvc.perform(get("/api/playlists"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].title").value("My List"));
  }

  @Test
  void getMyPlaylists_anonymous_unauthorized() throws Exception {
    mockMvc.perform(get("/api/playlists"))
        .andExpect(status().isUnauthorized());
  }

  // ── POST /api/playlists ───────────────────────────────────────────────────

  @Test
  @WithMockUser
  void create_authenticated_ok() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("New List", true);
    given(playlistService.create(any())).willReturn(sampleList());

    mockMvc.perform(post("/api/playlists")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("My List"));
  }

  @Test
  @WithMockUser
  void create_validation_fail_blankTitle() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("", true);

    mockMvc.perform(post("/api/playlists")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void create_anonymous_unauthorized() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("List", true);

    mockMvc.perform(post("/api/playlists")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized());
  }

  // ── GET /api/playlists/{id} ───────────────────────────────────────────────

  @Test
  void getDetail_public_anonymous_ok() throws Exception {
    given(playlistService.getDetail(1L)).willReturn(sampleDetail());

    mockMvc.perform(get("/api/playlists/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("My List"));
  }

  @Test
  void getDetail_private_anonymous_forbidden() throws Exception {
    given(playlistService.getDetail(1L))
        .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

    mockMvc.perform(get("/api/playlists/1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void getDetail_notFound() throws Exception {
    given(playlistService.getDetail(99L))
        .willThrow(new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    mockMvc.perform(get("/api/playlists/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("PLAYLIST_NOT_FOUND"));
  }

  // ── DELETE /api/playlists/{id} ────────────────────────────────────────────

  @Test
  @WithMockUser
  void delete_owner_ok() throws Exception {
    willDoNothing().given(playlistService).delete(1L);

    mockMvc.perform(delete("/api/playlists/1").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void delete_forbidden() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(playlistService).delete(2L);

    mockMvc.perform(delete("/api/playlists/2").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void delete_anonymous_unauthorized() throws Exception {
    mockMvc.perform(delete("/api/playlists/1"))
        .andExpect(status().isUnauthorized());
  }

  // ── POST /api/playlists/{id}/songs ────────────────────────────────────────

  @Test
  @WithMockUser
  void addSong_authenticated_ok() throws Exception {
    PlaylistSongAddRequest req = new PlaylistSongAddRequest(5L);
    willDoNothing().given(playlistService).addSong(eq(1L), any());

    mockMvc.perform(post("/api/playlists/1/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void addSong_songNotFound() throws Exception {
    PlaylistSongAddRequest req = new PlaylistSongAddRequest(99L);
    willThrow(new BusinessException(ErrorCode.SONG_NOT_FOUND))
        .given(playlistService).addSong(eq(1L), any());

    mockMvc.perform(post("/api/playlists/1/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("SONG_NOT_FOUND"));
  }

  @Test
  void addSong_anonymous_unauthorized() throws Exception {
    mockMvc.perform(post("/api/playlists/1/songs")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"songId\":5}"))
        .andExpect(status().isUnauthorized());
  }

  // ── DELETE /api/playlists/{id}/songs/{songId} ─────────────────────────────

  @Test
  @WithMockUser
  void removeSong_owner_ok() throws Exception {
    willDoNothing().given(playlistService).removeSong(1L, 5L);

    mockMvc.perform(delete("/api/playlists/1/songs/5").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void removeSong_forbidden() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN))
        .given(playlistService).removeSong(2L, 5L);

    mockMvc.perform(delete("/api/playlists/2/songs/5").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void removeSong_anonymous_unauthorized() throws Exception {
    mockMvc.perform(delete("/api/playlists/1/songs/5"))
        .andExpect(status().isUnauthorized());
  }
}
```

- [ ] **Step 2: Run test — expect compilation failure**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.controller.PlaylistControllerTest" 2>&1 | tail -20
```

Expected: compilation error — `PlaylistController` not found.

- [ ] **Step 3: Create `PlaylistController.java`**

```java
package com.vocaloidarchive.playlist.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistService playlistService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<PlaylistResponse>>> getMyPlaylists() {
    return ResponseEntity.ok(ApiResponse.success(playlistService.getMyPlaylists()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PlaylistResponse>> create(
      @RequestBody @Valid PlaylistCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(playlistService.create(req)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PlaylistDetailResponse>> getDetail(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(playlistService.getDetail(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    playlistService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/songs")
  public ResponseEntity<Void> addSong(
      @PathVariable Long id, @RequestBody @Valid PlaylistSongAddRequest req) {
    playlistService.addSong(id, req);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/songs/{songId}")
  public ResponseEntity<Void> removeSong(
      @PathVariable Long id, @PathVariable Long songId) {
    playlistService.removeSong(id, songId);
    return ResponseEntity.noContent().build();
  }
}
```

- [ ] **Step 4: Run `PlaylistControllerTest` — expect all green**

```
cd backend && ./gradlew test --tests "com.vocaloidarchive.playlist.controller.PlaylistControllerTest"
```

Expected: BUILD SUCCESSFUL, 15 tests passing.

- [ ] **Step 5: Run full test suite — final verification**

```
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests passing (≥143 total).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/playlist/controller/ \
        backend/src/test/java/com/vocaloidarchive/playlist/controller/
git commit -m "feat(playlist): add PlaylistController with 6 endpoints and WebMvcTest"
```

---

## Verification

After all 3 tasks are complete, run the full suite one final time:

```
cd backend && ./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests green.

Confirm task count: `./gradlew test | grep -E "tests|BUILD"` — should show 143+ tests.
