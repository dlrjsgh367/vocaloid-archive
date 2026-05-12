package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
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
  @Autowired UserJpaRepository userRepository;
  @PersistenceContext EntityManager em;

  @Test
  @DisplayName("findAllByUserId: 본인 플레이리스트만 반환")
  void findAllByUserId_returnsOwnerPlaylistsOnly() {
    UserEntity alice = userRepository.save(UserEntity.of("alice", "alice@a.com", "hash"));
    UserEntity bob   = userRepository.save(UserEntity.of("bob",   "bob@a.com",   "hash"));
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
    UserEntity u = userRepository.save(UserEntity.of("carol", "carol@a.com", "hash"));
    Playlist saved = playlistRepository.save(Playlist.of(u, "My List", false));
    em.flush();
    em.clear();

    Playlist found = playlistRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getTitle()).isEqualTo("My List");
    assertThat(found.isPublic()).isFalse();
  }
}
