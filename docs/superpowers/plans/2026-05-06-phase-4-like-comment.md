# Phase 4: Like + Comment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Like toggle and Comment CRUD endpoints, completing the social interaction layer of VocaloidArchive.

**Architecture:** Like domain already has `Like` entity + `LikeId` (Phase 3 artifacts); this phase adds repository/service/controller. Comment is a fresh domain with entity, repository, service, controller, and DTOs. No new Flyway migrations needed — both tables exist in V1.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, JUnit 5 + Mockito BDD, `@DataJpaTest` + Testcontainers MySQL, `@WebMvcTest` + Spring Security MockMvc.

---

## File Map

**Like — additions (Like.java + LikeId.java already exist from Phase 3):**

| Action | Path |
|--------|------|
| Modify | `backend/src/main/java/com/vocaloidarchive/like/domain/Like.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/like/repository/LikeRepository.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/like/dto/response/LikeToggleResponse.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/like/service/LikeService.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/like/controller/LikeController.java` |
| Create | `backend/src/test/java/com/vocaloidarchive/like/service/LikeServiceTest.java` |
| Create | `backend/src/test/java/com/vocaloidarchive/like/controller/LikeControllerTest.java` |

**Comment — all new:**

| Action | Path |
|--------|------|
| Create | `backend/src/main/java/com/vocaloidarchive/comment/domain/Comment.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/comment/repository/CommentRepository.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/comment/dto/request/CommentCreateRequest.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/comment/dto/response/CommentResponse.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/comment/service/CommentService.java` |
| Create | `backend/src/main/java/com/vocaloidarchive/comment/controller/CommentController.java` |
| Create | `backend/src/test/java/com/vocaloidarchive/comment/repository/CommentRepositoryTest.java` |
| Create | `backend/src/test/java/com/vocaloidarchive/comment/service/CommentServiceTest.java` |
| Create | `backend/src/test/java/com/vocaloidarchive/comment/controller/CommentControllerTest.java` |

---

## Key Domain Knowledge

**SecurityConfig (no changes needed):**
- `POST /api/songs/{id}/like` → covered by `anyRequest().authenticated()` ✓
- `GET /api/songs/{id}/comments` → covered by `GET /api/songs/**` permitAll ✓
- `POST /api/songs/{id}/comments`, `DELETE /api/comments/{id}` → `anyRequest().authenticated()` ✓

**SecurityUtil** is an `@Component` bean — inject via `@Autowired` / `@RequiredArgsConstructor`. Call as `securityUtil.getCurrentUserId()` (not static).

**PageResponse.from()** — factory is `from`, not `of`.

**Like entity likedAt** — `insertable = false, updatable = false`, relying on MySQL `DEFAULT CURRENT_TIMESTAMP` from V1 schema.

**Test patterns:**
- Controller slice: `@WebMvcTest(XController.class)` + `@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})`
- Repository integration: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Import(AuditingConfig.class)` + extend `AbstractMysqlContainerTest`
- Service unit: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`

---

## Task 1: Like entity factory + LikeRepository + LikeToggleResponse

**Files:**
- Modify: `backend/src/main/java/com/vocaloidarchive/like/domain/Like.java`
- Create: `backend/src/main/java/com/vocaloidarchive/like/repository/LikeRepository.java`
- Create: `backend/src/main/java/com/vocaloidarchive/like/dto/response/LikeToggleResponse.java`

- [ ] **Step 1: Add `Like.of()` factory to Like.java**

The existing entity has `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. Add a package-visible static factory inside the class. The full updated file:

```java
package com.vocaloidarchive.like.domain;

import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes")
@IdClass(LikeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Column(name = "liked_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime likedAt;

  public static Like of(User user, Song song) {
    Like like = new Like();
    like.user = user;
    like.song = song;
    return like;
  }
}
```

- [ ] **Step 2: Create LikeRepository**

```java
package com.vocaloidarchive.like.repository;

import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
  long countBySongId(Long songId);
}
```

`existsById(LikeId)` and `deleteById(LikeId)` are already provided by `JpaRepository`. `countBySongId` traverses `song.id` via Spring Data property path resolution.

- [ ] **Step 3: Create LikeToggleResponse**

```java
package com.vocaloidarchive.like.dto.response;

public record LikeToggleResponse(boolean liked, long likeCount) {}
```

- [ ] **Step 4: Compile check**

```bash
cd backend && ./gradlew compileJava -q
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/like/
git commit -m "feat(like): add Like factory, LikeRepository, LikeToggleResponse"
```

---

## Task 2: LikeService + unit tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/like/service/LikeService.java`
- Create: `backend/src/test/java/com/vocaloidarchive/like/service/LikeServiceTest.java`

- [ ] **Step 1: Write failing unit tests**

```java
package com.vocaloidarchive.like.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.repository.LikeRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

  @Mock LikeRepository likeRepository;
  @Mock SongRepository songRepository;
  @Mock UserRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks LikeService likeService;

  @Test
  void givenNotLiked_whenToggle_thenCreatesLikeAndReturnsLikedTrue() {
    // given
    Long userId = 1L, songId = 10L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(likeRepository.existsById(new LikeId(userId, songId))).willReturn(false);
    given(userRepository.getReferenceById(userId)).willReturn(User.of("u", "u@a.com", "h"));
    given(songRepository.getReferenceById(songId)).willReturn(
        Song.of(User.of("u", "u@a.com", "h"), "Title", null, null, null, null,
            com.vocaloidarchive.song.domain.Mood.BRIGHT));
    given(likeRepository.save(any(Like.class))).willAnswer(inv -> inv.getArgument(0));
    given(likeRepository.countBySongId(songId)).willReturn(5L);

    // when
    LikeToggleResponse res = likeService.toggle(songId);

    // then
    assertThat(res.liked()).isTrue();
    assertThat(res.likeCount()).isEqualTo(5L);
    then(likeRepository).should().save(any(Like.class));
  }

  @Test
  void givenAlreadyLiked_whenToggle_thenDeletesLikeAndReturnsLikedFalse() {
    // given
    Long userId = 1L, songId = 10L;
    LikeId likeId = new LikeId(userId, songId);
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(likeRepository.existsById(likeId)).willReturn(true);
    given(likeRepository.countBySongId(songId)).willReturn(3L);

    // when
    LikeToggleResponse res = likeService.toggle(songId);

    // then
    assertThat(res.liked()).isFalse();
    assertThat(res.likeCount()).isEqualTo(3L);
    then(likeRepository).should().deleteById(likeId);
  }

  @Test
  void givenSongNotFound_whenToggle_thenThrowsSongNotFound() {
    // given
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(songRepository.existsById(999L)).willReturn(false);

    // when / then
    assertThatThrownBy(() -> likeService.toggle(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.like.service.LikeServiceTest" -q 2>&1 | tail -20
```

Expected: compilation error or test failure (LikeService doesn't exist yet).

- [ ] **Step 3: Create LikeService**

```java
package com.vocaloidarchive.like.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.repository.LikeRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

  private final LikeRepository likeRepository;
  private final SongRepository songRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public LikeToggleResponse toggle(Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    LikeId likeId = new LikeId(userId, songId);
    boolean currentlyLiked = likeRepository.existsById(likeId);
    if (currentlyLiked) {
      likeRepository.deleteById(likeId);
    } else {
      User user = userRepository.getReferenceById(userId);
      Song song = songRepository.getReferenceById(songId);
      likeRepository.save(Like.of(user, song));
    }
    long likeCount = likeRepository.countBySongId(songId);
    return new LikeToggleResponse(!currentlyLiked, likeCount);
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.like.service.LikeServiceTest" -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/like/service/ \
        backend/src/test/java/com/vocaloidarchive/like/service/
git commit -m "feat(like): add LikeService toggle with unit tests"
```

---

## Task 3: LikeController + WebMvcTest

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/like/controller/LikeController.java`
- Create: `backend/src/test/java/com/vocaloidarchive/like/controller/LikeControllerTest.java`

- [ ] **Step 1: Write failing controller test**

```java
package com.vocaloidarchive.like.controller;

import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.service.LikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikeController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class LikeControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean LikeService likeService;

  @Test
  @WithMockUser
  void toggle_authenticated_returns200WithLikeData() throws Exception {
    given(likeService.toggle(1L)).willReturn(new LikeToggleResponse(true, 5L));

    mockMvc.perform(post("/api/songs/1/like").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.liked").value(true))
        .andExpect(jsonPath("$.data.likeCount").value(5));
  }

  @Test
  void toggle_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/songs/1/like").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void toggle_songNotFound_returns404() throws Exception {
    given(likeService.toggle(999L))
        .willThrow(new BusinessException(ErrorCode.SONG_NOT_FOUND));

    mockMvc.perform(post("/api/songs/999/like").with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("SONG_NOT_FOUND"));
  }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.like.controller.LikeControllerTest" -q 2>&1 | tail -20
```

Expected: compilation error (LikeController doesn't exist).

- [ ] **Step 3: Create LikeController**

```java
package com.vocaloidarchive.like.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.service.LikeService;
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

  private final LikeService likeService;

  @PostMapping("/{id}/like")
  public ResponseEntity<ApiResponse<LikeToggleResponse>> toggle(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(likeService.toggle(id)));
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.like.controller.LikeControllerTest" -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/like/controller/ \
        backend/src/test/java/com/vocaloidarchive/like/controller/
git commit -m "feat(like): add LikeController POST /api/songs/{id}/like with WebMvcTest"
```

---

## Task 4: Comment entity + CommentRepository + @DataJpaTest

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/comment/domain/Comment.java`
- Create: `backend/src/main/java/com/vocaloidarchive/comment/repository/CommentRepository.java`
- Create: `backend/src/test/java/com/vocaloidarchive/comment/repository/CommentRepositoryTest.java`

- [ ] **Step 1: Create Comment entity**

```java
package com.vocaloidarchive.comment.domain;

import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Column(nullable = false, length = 500)
  private String content;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public static Comment of(User user, Song song, String content) {
    Comment c = new Comment();
    c.user = user;
    c.song = song;
    c.content = content;
    return c;
  }
}
```

- [ ] **Step 2: Create CommentRepository**

The query uses `JOIN FETCH c.user` to avoid N+1 queries when converting comments to DTOs. A separate `countQuery` is required because JPQL with `JOIN FETCH` cannot be used in count queries.

```java
package com.vocaloidarchive.comment.repository;

import com.vocaloidarchive.comment.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  @Query(
      value = "SELECT c FROM Comment c JOIN FETCH c.user WHERE c.song.id = :songId ORDER BY c.createdAt DESC",
      countQuery = "SELECT count(c) FROM Comment c WHERE c.song.id = :songId"
  )
  Page<Comment> findWithUserBySongId(@Param("songId") Long songId, Pageable pageable);

  boolean existsByIdAndUserId(Long id, Long userId);
}
```

`existsByIdAndUserId` traverses `user.id` for the ownership check in delete.

- [ ] **Step 3: Write failing repository integration tests**

```java
package com.vocaloidarchive.comment.repository;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class CommentRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired CommentRepository commentRepository;
  @Autowired UserRepository userRepository;
  @Autowired SongRepository songRepository;

  private User user1;
  private User user2;
  private Song song;

  @BeforeEach
  void setUp() {
    commentRepository.deleteAll();
    user1 = userRepository.save(User.of("alice", "alice@a.com", "hash"));
    user2 = userRepository.save(User.of("bob", "bob@b.com", "hash"));
    song = songRepository.save(Song.of(user1, "Song Title", null, null, null, null, Mood.BRIGHT));
  }

  @Test
  @DisplayName("findWithUserBySongId: 최신순으로 페이징 반환, user 즉시 로딩")
  void findWithUserBySongId_returnsPaginatedNewestFirst() {
    Comment c1 = commentRepository.save(Comment.of(user1, song, "first"));
    Comment c2 = commentRepository.save(Comment.of(user2, song, "second"));

    Page<Comment> page = commentRepository.findWithUserBySongId(song.getId(), PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent().get(0).getId()).isEqualTo(c2.getId());
    assertThat(page.getContent().get(1).getId()).isEqualTo(c1.getId());
    // user already loaded (no LazyInitializationException after session close)
    assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo("bob");
  }

  @Test
  @DisplayName("existsByIdAndUserId: 소유자 true, 타인 false")
  void existsByIdAndUserId_ownerReturnsTrue_otherReturnsFalse() {
    Comment comment = commentRepository.save(Comment.of(user1, song, "content"));

    assertThat(commentRepository.existsByIdAndUserId(comment.getId(), user1.getId())).isTrue();
    assertThat(commentRepository.existsByIdAndUserId(comment.getId(), user2.getId())).isFalse();
  }
}
```

- [ ] **Step 4: Run to verify they fail**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.repository.CommentRepositoryTest" -q 2>&1 | tail -20
```

Expected: compilation error (Comment/CommentRepository don't exist yet — but they do now, so expect test to run and possibly fail if setup is wrong).

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.repository.CommentRepositoryTest" -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 2 tests passing.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/comment/domain/ \
        backend/src/main/java/com/vocaloidarchive/comment/repository/ \
        backend/src/test/java/com/vocaloidarchive/comment/repository/
git commit -m "feat(comment): add Comment entity, CommentRepository with @DataJpaTest"
```

---

## Task 5: Comment DTOs + CommentService + unit tests

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/comment/dto/request/CommentCreateRequest.java`
- Create: `backend/src/main/java/com/vocaloidarchive/comment/dto/response/CommentResponse.java`
- Create: `backend/src/main/java/com/vocaloidarchive/comment/service/CommentService.java`
- Create: `backend/src/test/java/com/vocaloidarchive/comment/service/CommentServiceTest.java`

- [ ] **Step 1: Create CommentCreateRequest**

```java
package com.vocaloidarchive.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
    @NotBlank @Size(min = 1, max = 500) String content
) {}
```

- [ ] **Step 2: Create CommentResponse**

```java
package com.vocaloidarchive.comment.dto.response;

import com.vocaloidarchive.comment.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String content,
    String username,
    LocalDateTime createdAt
) {
  public static CommentResponse from(Comment c) {
    return new CommentResponse(
        c.getId(),
        c.getContent(),
        c.getUser().getUsername(),
        c.getCreatedAt()
    );
  }
}
```

- [ ] **Step 3: Write failing service unit tests**

```java
package com.vocaloidarchive.comment.service;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.repository.CommentRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock CommentRepository commentRepository;
  @Mock SongRepository songRepository;
  @Mock UserRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks CommentService commentService;

  private User makeUser(Long id, String name) {
    return User.of(name, name + "@a.com", "hash");
  }

  private Song makeSong(User owner) {
    return Song.of(owner, "Title", null, null, null, null, Mood.BRIGHT);
  }

  @Test
  void givenSongExists_whenList_thenReturnsPageResponse() {
    User user = makeUser(1L, "alice");
    Song song = makeSong(user);
    Comment comment = Comment.of(user, song, "hello");
    PageRequest pageable = PageRequest.of(0, 20);

    given(songRepository.existsById(10L)).willReturn(true);
    given(commentRepository.findWithUserBySongId(10L, pageable))
        .willReturn(new PageImpl<>(List.of(comment), pageable, 1));

    PageResponse<CommentResponse> result = commentService.list(10L, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).content()).isEqualTo("hello");
  }

  @Test
  void givenSongNotFound_whenList_thenThrows() {
    given(songRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.list(999L, PageRequest.of(0, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  @Test
  void givenValidRequest_whenCreate_thenSavesComment() {
    Long userId = 1L, songId = 10L;
    User user = makeUser(userId, "alice");
    Song song = makeSong(user);
    Comment comment = Comment.of(user, song, "great song");
    CommentCreateRequest req = new CommentCreateRequest("great song");

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(songRepository.getReferenceById(songId)).willReturn(song);
    given(commentRepository.save(any(Comment.class))).willReturn(comment);

    CommentResponse res = commentService.create(songId, req);

    assertThat(res.content()).isEqualTo("great song");
    then(commentRepository).should().save(any(Comment.class));
  }

  @Test
  void givenSongNotFound_whenCreate_thenThrows() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(songRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.create(999L, new CommentCreateRequest("text")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  @Test
  void givenOwner_whenDelete_thenDeletesComment() {
    Long userId = 1L, commentId = 5L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(commentRepository.existsById(commentId)).willReturn(true);
    given(commentRepository.existsByIdAndUserId(commentId, userId)).willReturn(true);

    commentService.delete(commentId);

    then(commentRepository).should().deleteById(commentId);
  }

  @Test
  void givenNotOwner_whenDelete_thenThrowsForbidden() {
    Long userId = 2L, commentId = 5L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(commentRepository.existsById(commentId)).willReturn(true);
    given(commentRepository.existsByIdAndUserId(commentId, userId)).willReturn(false);

    assertThatThrownBy(() -> commentService.delete(commentId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void givenCommentNotFound_whenDelete_thenThrows() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(commentRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.delete(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
  }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.service.CommentServiceTest" -q 2>&1 | tail -20
```

Expected: compilation error (CommentService doesn't exist yet).

- [ ] **Step 5: Create CommentService**

```java
package com.vocaloidarchive.comment.service;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.repository.CommentRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final SongRepository songRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  public PageResponse<CommentResponse> list(Long songId, Pageable pageable) {
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    return PageResponse.from(
        commentRepository.findWithUserBySongId(songId, pageable)
            .map(CommentResponse::from));
  }

  @Transactional
  public CommentResponse create(Long songId, CommentCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    var user = userRepository.getReferenceById(userId);
    var song = songRepository.getReferenceById(songId);
    Comment saved = commentRepository.save(Comment.of(user, song, req.content()));
    return CommentResponse.from(saved);
  }

  @Transactional
  public void delete(Long commentId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!commentRepository.existsById(commentId)) {
      throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
    }
    if (!commentRepository.existsByIdAndUserId(commentId, userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    commentRepository.deleteById(commentId);
  }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.service.CommentServiceTest" -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 7 tests passing.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/comment/
git commit -m "feat(comment): add CommentService with DTOs and unit tests"
```

---

## Task 6: CommentController + WebMvcTest + final build

**Files:**
- Create: `backend/src/main/java/com/vocaloidarchive/comment/controller/CommentController.java`
- Create: `backend/src/test/java/com/vocaloidarchive/comment/controller/CommentControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

```java
package com.vocaloidarchive.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.service.CommentService;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class CommentControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean CommentService commentService;

  private CommentResponse sampleComment() {
    return new CommentResponse(1L, "great song", "alice", LocalDateTime.now());
  }

  @Test
  void listComments_anonymous_returns200() throws Exception {
    PageResponse<CommentResponse> page = PageResponse.from(
        new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1));
    given(commentService.list(eq(1L), any())).willReturn(page);

    mockMvc.perform(get("/api/songs/1/comments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].content").value("great song"));
  }

  @Test
  @WithMockUser
  void createComment_authenticated_returns201() throws Exception {
    given(commentService.create(eq(1L), any())).willReturn(sampleComment());

    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest("great song"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.content").value("great song"));
  }

  @Test
  void createComment_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest("text"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void createComment_blankContent_returns400() throws Exception {
    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest(""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  @WithMockUser
  void deleteComment_owner_returns204() throws Exception {
    willDoNothing().given(commentService).delete(1L);

    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteComment_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void deleteComment_notOwner_returns403() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(commentService).delete(1L);

    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }
}
```

- [ ] **Step 2: Run to verify they fail**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.controller.CommentControllerTest" -q 2>&1 | tail -20
```

Expected: compilation error (CommentController doesn't exist).

- [ ] **Step 3: Create CommentController**

```java
package com.vocaloidarchive.comment.controller;

import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.service.CommentService;
import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @GetMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> list(
      @PathVariable Long id,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.success(commentService.list(id, pageable)));
  }

  @PostMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<CommentResponse>> create(
      @PathVariable Long id,
      @RequestBody @Valid CommentCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(commentService.create(id, req)));
  }

  @DeleteMapping("/api/comments/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    commentService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
```

- [ ] **Step 4: Run comment controller tests to verify they pass**

```bash
cd backend && ./gradlew test --tests "com.vocaloidarchive.comment.controller.CommentControllerTest" -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 7 tests passing.

- [ ] **Step 5: Run full test suite**

```bash
cd backend && ./gradlew test -q 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests passing (previous 105 + new ~17 = ~122 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/vocaloidarchive/comment/controller/ \
        backend/src/test/java/com/vocaloidarchive/comment/controller/
git commit -m "feat(comment): add CommentController with WebMvcTest"
```

---

## Final Verification

- [ ] **Build the full artifact**

```bash
cd backend && ./gradlew clean build -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, `build/libs/backend-0.0.1-SNAPSHOT.jar` produced.

- [ ] **Run full test suite one more time**

```bash
cd backend && ./gradlew test -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Commit plan completion marker**

```bash
git add docs/superpowers/plans/2026-05-06-phase-4-like-comment.md
git commit -m "docs(phase-4): mark Phase 4 plan complete"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Covered by |
|---|---|
| POST /api/songs/{id}/like → toggle, return `{liked, likeCount}` | Task 2 (service) + Task 3 (controller) |
| GET /api/songs/{id}/comments?page= → 최신순 PageResponse | Task 4 (repo) + Task 5 (service) + Task 6 (controller) |
| POST /api/songs/{id}/comments `{content}` 1-500자 | Task 5 (CommentCreateRequest @Size) + Task 6 |
| DELETE /api/comments/{id} 본인만 | Task 5 (service FORBIDDEN) + Task 6 |
| Like: 인증 필요 | SecurityConfig anyRequest().authenticated() — no change needed |
| Comment GET: 비인증 허용 | SecurityConfig GET /api/songs/** permitAll — no change needed |
| Comment POST/DELETE: 인증 필요 | SecurityConfig anyRequest().authenticated() — no change needed |

**Placeholder scan:** No TBD/TODO found. All steps include complete code.

**Type consistency:**
- `LikeToggleResponse(boolean liked, long likeCount)` — consistent in service and controller tests.
- `CommentResponse.from(Comment c)` → uses `c.getUser().getUsername()` — user is JOIN FETCHed in repo, so no lazy init issue.
- `PageResponse.from(page.map(...))` — uses `.map()` variant; `PageResponse.from` accepts `Page<T>`.
- `CommentService.list` returns `PageResponse<CommentResponse>` — matches controller and test.
- `commentService.delete(commentId)` signature: `void delete(Long commentId)` — matches controller `@PathVariable Long id`.
