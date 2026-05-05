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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class CommentRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired CommentRepository commentRepository;
  @Autowired UserRepository userRepository;
  @Autowired SongRepository songRepository;
  @Autowired TestEntityManager em;

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
  void findWithUserBySongId_returnsPaginatedNewestFirst() throws InterruptedException {
    Comment c1 = commentRepository.save(Comment.of(user1, song, "first"));
    // flush now so c1 gets a createdAt before the sleep
    em.flush();
    // TIMESTAMP precision is 1 second in MySQL — sleep past 1 s boundary
    Thread.sleep(1100);
    Comment c2 = commentRepository.save(Comment.of(user2, song, "second"));
    em.flush();

    Page<Comment> page = commentRepository.findWithUserBySongId(song.getId(),
        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

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
