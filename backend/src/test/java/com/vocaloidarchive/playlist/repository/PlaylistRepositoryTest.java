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
