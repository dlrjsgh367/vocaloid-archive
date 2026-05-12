package com.vocaloidarchive.song.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.tag.repository.TagRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditingConfig.class)
class SongRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired SongRepository songRepository;
  @Autowired UserJpaRepository userRepository;
  @Autowired CharacterJpaRepository characterRepository;
  @Autowired TagRepository tagRepository;
  @PersistenceContext EntityManager em;

  @Test
  @DisplayName("incrementPlayCount: 영향 row=1, DB에서 +1 반영")
  void incrementPlayCount_increments() {
    UserEntity u = userRepository.save(UserEntity.of("alice", "alice@a.com", "hash"));
    Song s = songRepository.save(Song.of(u, "Title", null, null, null, null, Mood.BRIGHT));
    em.flush();

    int affected = songRepository.incrementPlayCount(s.getId());

    em.clear();
    Song reloaded = songRepository.findById(s.getId()).orElseThrow();
    assertThat(affected).isEqualTo(1);
    assertThat(reloaded.getPlayCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("incrementPlayCount: 없는 id → 영향 row=0")
  void incrementPlayCount_missing() {
    int affected = songRepository.incrementPlayCount(999_999L);
    assertThat(affected).isEqualTo(0);
  }

  @Test
  @DisplayName("findDetailWithCharacters: LAZY 초기화 없이 character 컬렉션 접근")
  void findDetailWithCharacters_fetchJoin() {
    UserEntity u = userRepository.save(UserEntity.of("bob", "bob@a.com", "hash"));
    CharacterEntity c = characterRepository.save(CharacterEntity.of("미쿠", "#39C5BB", null));
    Song s = Song.of(u, "Title", null, null, null, null, Mood.CALM);
    s.addCharacter(c);
    songRepository.save(s);
    em.flush();
    em.clear();

    Optional<Song> result = songRepository.findDetailWithCharacters(s.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getCharacters()).hasSize(1);
    assertThat(result.get().getCharacters().get(0).getCharacter().getName()).isEqualTo("미쿠");
  }

  @Test
  @DisplayName("findDetailWithTags: tags 컬렉션 fetch join")
  void findDetailWithTags_fetchJoin() {
    UserEntity u = userRepository.save(UserEntity.of("carol", "c@a.com", "hash"));
    Tag t = tagRepository.save(Tag.of("pop"));
    Song s = Song.of(u, "Title", null, null, null, null, Mood.DARK);
    s.addTag(t);
    songRepository.save(s);
    em.flush();
    em.clear();

    Optional<Song> result = songRepository.findDetailWithTags(s.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getTags()).hasSize(1);
    assertThat(result.get().getTags().get(0).getTag().getName()).isEqualTo("pop");
  }

  @Test
  @DisplayName("Song 삭제 시 song_characters / song_tags CASCADE")
  void delete_cascades_join_tables() {
    UserEntity u = userRepository.save(UserEntity.of("dave", "d@a.com", "hash"));
    CharacterEntity c = characterRepository.save(CharacterEntity.of("렌", "#FFC56C", null));
    Tag t = tagRepository.save(Tag.of("rock"));
    Song s = Song.of(u, "Title", null, null, null, null, Mood.ENERGETIC);
    s.addCharacter(c);
    s.addTag(t);
    songRepository.save(s);
    em.flush();
    Long id = s.getId();

    songRepository.delete(s);
    em.flush();
    em.clear();

    assertThat(songRepository.findById(id)).isEmpty();
    Number scCount = (Number) em.createNativeQuery(
        "select count(*) from song_characters where song_id = ?")
        .setParameter(1, id).getSingleResult();
    Number stCount = (Number) em.createNativeQuery(
        "select count(*) from song_tags where song_id = ?")
        .setParameter(1, id).getSingleResult();
    assertThat(scCount.longValue()).isZero();
    assertThat(stCount.longValue()).isZero();
  }
}
