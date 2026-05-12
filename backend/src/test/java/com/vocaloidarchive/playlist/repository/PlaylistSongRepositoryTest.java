package com.vocaloidarchive.playlist.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.playlist.infra.persistence.PlaylistEntity;
import com.vocaloidarchive.playlist.infra.persistence.PlaylistJpaRepository;
import com.vocaloidarchive.playlist.infra.persistence.PlaylistSongEntity;
import com.vocaloidarchive.playlist.infra.persistence.PlaylistSongJpaRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
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

  @Autowired PlaylistJpaRepository playlistRepository;
  @Autowired PlaylistSongJpaRepository playlistSongRepository;
  @Autowired SongRepository songRepository;
  @Autowired UserJpaRepository userRepository;
  @PersistenceContext EntityManager em;

  private PlaylistEntity playlist;
  private Song song1;
  private Song song2;

  @BeforeEach
  void setUp() {
    UserEntity u = userRepository.save(UserEntity.of("test", "t@a.com", "hash"));
    playlist = playlistRepository.save(PlaylistEntity.of(u, "My List", true));
    song1 = songRepository.save(Song.of(u, "Song 1", null, null, null, null, Mood.BRIGHT));
    song2 = songRepository.save(Song.of(u, "Song 2", null, null, null, null, Mood.CALM));
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("findWithSongByPlaylistId: order_index 오름차순으로 반환")
  void findWithSongByPlaylistId_orderedAsc() {
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song2, 2));
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song1, 1));
    em.flush();
    em.clear();

    List<PlaylistSongEntity> result = playlistSongRepository.findWithSongByPlaylistId(playlist.getId());

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getSong().getTitle()).isEqualTo("Song 1");
    assertThat(result.get(1).getSong().getTitle()).isEqualTo("Song 2");
  }

  @Test
  @DisplayName("findMaxOrderIndexByPlaylistId: 비어있으면 0, 있으면 최대값")
  void findMaxOrderIndexByPlaylistId() {
    assertThat(playlistSongRepository.findMaxOrderIndexByPlaylistId(playlist.getId())).isEqualTo(0);

    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song1, 1));
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song2, 3));
    em.flush();

    assertThat(playlistSongRepository.findMaxOrderIndexByPlaylistId(playlist.getId())).isEqualTo(3);
  }

  @Test
  @DisplayName("existsByPlaylistIdAndSongId: 존재 여부 확인")
  void existsByPlaylistIdAndSongId() {
    assertThat(playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), song1.getId())).isFalse();
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song1, 1));
    em.flush();
    assertThat(playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), song1.getId())).isTrue();
  }

  @Test
  @DisplayName("deleteByPlaylistIdAndSongId: 해당 row 삭제")
  void deleteByPlaylistIdAndSongId_removesRow() {
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song1, 1));
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
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song1, 1));
    playlistSongRepository.save(PlaylistSongEntity.of(playlist, song2, 2));
    em.flush();
    assertThat(playlistSongRepository.countByPlaylistId(playlist.getId())).isEqualTo(2L);
  }
}
