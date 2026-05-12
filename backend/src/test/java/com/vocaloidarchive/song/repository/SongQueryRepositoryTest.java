package com.vocaloidarchive.song.repository;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.common.config.JpaConfig;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.dto.request.SongSearchRequest;
import com.vocaloidarchive.song.dto.request.SongSort;
import com.vocaloidarchive.tag.infra.persistence.TagEntity;
import com.vocaloidarchive.tag.infra.persistence.TagJpaRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SongQueryRepository.class, JpaConfig.class, AuditingConfig.class})
class SongQueryRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired SongQueryRepository songQueryRepository;
  @Autowired SongRepository songRepository;
  @Autowired UserJpaRepository userRepository;
  @Autowired CharacterJpaRepository characterRepository;
  @Autowired TagJpaRepository tagRepository;
  @PersistenceContext EntityManager em;

  CharacterEntity miku, len, kaito;
  TagEntity pop, rock, anime;
  Song s1, s2, s3, s4;

  @BeforeEach
  void setUp() {
    UserEntity u = userRepository.save(UserEntity.of("alice", "alice@a.com", "hash"));
    // V2 migration already seeds characters — fetch them instead of inserting duplicates
    List<CharacterEntity> chars = characterRepository.findAllByOrderByIdAsc();
    miku  = chars.get(0);  // 하츠네 미쿠
    len   = chars.get(2);  // 카가미네 렌
    kaito = chars.get(4);  // KAITO
    pop   = tagRepository.save(TagEntity.of("pop"));
    rock  = tagRepository.save(TagEntity.of("rock"));
    anime = tagRepository.save(TagEntity.of("anime"));

    s1 = newSong(u, "Bright Day",   Mood.BRIGHT,    miku, pop);
    s2 = newSong(u, "Dark Night",   Mood.DARK,      len,  rock);
    s3 = newSong(u, "Calm Stream",  Mood.CALM,      kaito, anime);
    s4 = newSong(u, "Energetic Mix", Mood.ENERGETIC, miku, rock);

    em.flush();
    em.clear();
  }

  private Song newSong(UserEntity u, String title, Mood mood, CharacterEntity c, TagEntity t) {
    Song s = Song.of(u, title, null, null, null, null, mood);
    s.addCharacter(c);
    s.addTag(t);
    return songRepository.save(s);
  }

  private SongSearchRequest req(String kw, Mood mood, Long charId, Long tagId, SongSort sort) {
    return new SongSearchRequest(kw, mood, charId, tagId, sort);
  }

  @Test
  @DisplayName("필터 없음 + sort=latest → 4곡 createdAt desc, id desc")
  void noFilter() {
    Page<Song> page = songQueryRepository.search(
        req(null, null, null, null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getTotalElements()).isEqualTo(4);
    assertThat(page.getContent()).extracting(Song::getId)
        .containsExactly(s4.getId(), s3.getId(), s2.getId(), s1.getId());
  }

  @Test
  @DisplayName("keyword=title 일부 → title 매치")
  void keyword_title() {
    Page<Song> page = songQueryRepository.search(
        req("Bright", null, null, null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId).containsExactly(s1.getId());
  }

  @Test
  @DisplayName("keyword=character.name 일부 → 매핑된 곡만")
  void keyword_character() {
    Page<Song> page = songQueryRepository.search(
        req("KAITO", null, null, null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId).containsExactly(s3.getId());
  }

  @Test
  @DisplayName("keyword=tag.name 일부 → 매핑된 곡만")
  void keyword_tag() {
    Page<Song> page = songQueryRepository.search(
        req("anime", null, null, null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId).containsExactly(s3.getId());
  }

  @Test
  @DisplayName("mood=BRIGHT")
  void mood() {
    Page<Song> page = songQueryRepository.search(
        req(null, Mood.BRIGHT, null, null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId).containsExactly(s1.getId());
  }

  @Test
  @DisplayName("characterId 필터")
  void characterId() {
    Page<Song> page = songQueryRepository.search(
        req(null, null, miku.getId(), null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId)
        .containsExactlyInAnyOrder(s1.getId(), s4.getId());
  }

  @Test
  @DisplayName("tagId 필터")
  void tagId() {
    Page<Song> page = songQueryRepository.search(
        req(null, null, null, rock.getId(), SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId)
        .containsExactlyInAnyOrder(s2.getId(), s4.getId());
  }

  @Test
  @DisplayName("mood + characterId 동시 (AND)")
  void mood_and_characterId() {
    Page<Song> page = songQueryRepository.search(
        req(null, Mood.ENERGETIC, miku.getId(), null, SongSort.LATEST), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId).containsExactly(s4.getId());
  }

  @Test
  @DisplayName("sort=played → playCount desc")
  void sort_played() {
    songRepository.incrementPlayCount(s2.getId());
    songRepository.incrementPlayCount(s2.getId());
    songRepository.incrementPlayCount(s3.getId());
    em.flush(); em.clear();

    Page<Song> page = songQueryRepository.search(
        req(null, null, null, null, SongSort.PLAYED), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId)
        .containsExactly(s2.getId(), s3.getId(), s4.getId(), s1.getId());
  }

  @Test
  @DisplayName("sort=popular (likes 0) → 동률 → tiebreaker createdAt desc")
  void sort_popular_zero_likes() {
    Page<Song> page = songQueryRepository.search(
        req(null, null, null, null, SongSort.POPULAR), PageRequest.of(0, 20));
    assertThat(page.getContent()).extracting(Song::getId)
        .containsExactly(s4.getId(), s3.getId(), s2.getId(), s1.getId());
  }

  @Test
  @DisplayName("sort=popular (likes INSERT 후) → likeCount desc")
  void sort_popular_with_likes() {
    em.createNativeQuery("insert into likes (user_id, song_id) values (?, ?)")
        .setParameter(1, userRepository.findAll().get(0).getId())
        .setParameter(2, s2.getId())
        .executeUpdate();
    em.flush(); em.clear();

    Page<Song> page = songQueryRepository.search(
        req(null, null, null, null, SongSort.POPULAR), PageRequest.of(0, 20));
    assertThat(page.getContent().get(0).getId()).isEqualTo(s2.getId());
  }

  @Test
  @DisplayName("페이지 size=2 → totalElements=4, totalPages=2")
  void pagination() {
    Page<Song> page = songQueryRepository.search(
        req(null, null, null, null, SongSort.LATEST), PageRequest.of(0, 2));
    assertThat(page.getTotalElements()).isEqualTo(4);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getContent()).hasSize(2);
  }

  @Test
  @DisplayName("likeCountsFor: songIds 매핑, 누락 id는 0")
  void likeCountsFor_zeros() {
    Map<Long, Long> map = songQueryRepository.likeCountsFor(
        List.of(s1.getId(), s2.getId()));
    assertThat(map).containsEntry(s1.getId(), 0L).containsEntry(s2.getId(), 0L);
  }
}
