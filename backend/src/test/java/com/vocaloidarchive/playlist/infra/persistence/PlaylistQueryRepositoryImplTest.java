package com.vocaloidarchive.playlist.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vocaloidarchive.common.config.AuditingConfig;
import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.infra.persistence.SongEntity;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditingConfig.class, PlaylistQueryRepositoryImpl.class})
class PlaylistQueryRepositoryImplTest extends AbstractMysqlContainerTest {

  @Autowired PlaylistQueryRepositoryImpl queryRepo;
  @PersistenceContext EntityManager em;

  private UserEntity user(String name) {
    UserEntity u = UserEntity.of(name, name + "@a.com", "hash");
    em.persist(u);
    return u;
  }

  private CharacterEntity character(String name, String colorHex) {
    CharacterEntity c = CharacterEntity.of(name, colorHex, null);
    em.persist(c);
    return c;
  }

  private SongEntity song(UserEntity owner, String title, Mood mood, CharacterEntity... chars) {
    SongEntity s = SongEntity.of(owner, title, "https://youtu.be/abcdefghijk", null, null, null, mood);
    for (CharacterEntity c : chars) s.addCharacter(c);
    em.persist(s);
    return s;
  }

  private PlaylistEntity playlist(UserEntity owner, String title, boolean isPublic, String code) {
    PlaylistEntity p = PlaylistEntity.of(owner, title, isPublic);
    p.assignShareCode(code);
    em.persist(p);
    return p;
  }

  private void addSong(PlaylistEntity p, SongEntity s, int order) {
    em.persist(PlaylistSongEntity.of(p, s, order));
  }

  private void like(UserEntity u, SongEntity s) {
    em.createNativeQuery("INSERT INTO likes (user_id, song_id) VALUES (:u, :s)")
        .setParameter("u", u.getId())
        .setParameter("s", s.getId())
        .executeUpdate();
  }

  @Test
  @DisplayName("findCardDataByShareCode: 곡 순서·대표 캐릭터 색·likeSum 정확")
  void findCardData_full() {
    UserEntity owner = user("alice");
    UserEntity fan = user("bob");
    CharacterEntity miku = character("TestMiku", "#39C5BB");
    CharacterEntity luka = character("TestLuka", "#FF9DC0");

    // miku appears on 3 songs, luka on 1 → miku is the primary
    SongEntity s1 = song(owner, "Song1", Mood.ENERGETIC, miku);
    SongEntity s2 = song(owner, "Song2", Mood.CALM, miku);
    SongEntity s3 = song(owner, "Song3", Mood.BRIGHT, miku);
    SongEntity s4 = song(owner, "Song4", Mood.EMOTIONAL, luka);

    PlaylistEntity p = playlist(owner, "My List", true, "CODE000001");
    addSong(p, s2, 2);
    addSong(p, s1, 1);
    addSong(p, s4, 4);
    addSong(p, s3, 3);

    // likes: s1 x2, s2 x1 → likeSum = 3
    like(owner, s1);
    like(fan, s1);
    like(fan, s2);

    em.flush();
    em.clear();

    Optional<PlaylistCardData> result = queryRepo.findCardDataByShareCode("CODE000001");

    assertThat(result).isPresent();
    PlaylistCardData card = result.get();
    assertThat(card.title()).isEqualTo("My List");
    assertThat(card.ownerUsername()).isEqualTo("alice");
    assertThat(card.isPublic()).isTrue();
    assertThat(card.songCount()).isEqualTo(4);
    assertThat(card.likeSum()).isEqualTo(3L);
    assertThat(card.themeColorHex()).isEqualTo("#39C5BB");
    assertThat(card.primaryCharName()).isEqualTo("TestMiku");
    // ordered by orderIndex asc
    assertThat(card.songs()).extracting(PlaylistCardData.SongLine::title)
        .containsExactly("Song1", "Song2", "Song3", "Song4");
    // mood + thumbnail derived from youtube
    assertThat(card.songs().get(0).mood()).isEqualTo("ENERGETIC");
    assertThat(card.songs().get(0).thumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg");
  }

  @Test
  @DisplayName("findCardDataByShareCode: 캐릭터 동률 → character.id 작은 쪽")
  void findCardData_tieBreak_lowerId() {
    UserEntity owner = user("carol");
    CharacterEntity first = character("TestFirst", "#111111");   // lower id
    CharacterEntity second = character("TestSecond", "#222222");

    SongEntity s1 = song(owner, "S1", Mood.BRIGHT, first);
    SongEntity s2 = song(owner, "S2", Mood.DARK, second);

    PlaylistEntity p = playlist(owner, "Tie", true, "CODE000002");
    addSong(p, s1, 1);
    addSong(p, s2, 2);
    em.flush();
    em.clear();

    PlaylistCardData card = queryRepo.findCardDataByShareCode("CODE000002").orElseThrow();
    assertThat(card.themeColorHex()).isEqualTo("#111111");
    assertThat(card.primaryCharName()).isEqualTo("TestFirst");
  }

  @Test
  @DisplayName("findCardDataByShareCode: 캐릭터 없는 플리 → 미쿠 기본색, 좋아요 0")
  void findCardData_noCharacter_default() {
    UserEntity owner = user("dave");
    SongEntity s1 = song(owner, "Lonely", Mood.CALM); // no characters

    PlaylistEntity p = playlist(owner, "NoChar", true, "CODE000003");
    addSong(p, s1, 1);
    em.flush();
    em.clear();

    PlaylistCardData card = queryRepo.findCardDataByShareCode("CODE000003").orElseThrow();
    assertThat(card.themeColorHex()).isEqualTo("#39C5BB");
    assertThat(card.primaryCharName()).isNull();
    assertThat(card.likeSum()).isZero();
    assertThat(card.songCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("findCardDataByShareCode: 없는 code → empty")
  void findCardData_unknownCode_empty() {
    assertThat(queryRepo.findCardDataByShareCode("NOPENOPE12")).isEmpty();
  }

  @Test
  @DisplayName("findCardDataByShareCode: 비공개 플리도 조회는 됨(권한은 use case 책임)")
  void findCardData_private_stillReturned() {
    UserEntity owner = user("erin");
    SongEntity s1 = song(owner, "Secret", Mood.DARK);
    PlaylistEntity p = playlist(owner, "Private", false, "CODE000004");
    addSong(p, s1, 1);
    em.flush();
    em.clear();

    PlaylistCardData card = queryRepo.findCardDataByShareCode("CODE000004").orElseThrow();
    assertThat(card.isPublic()).isFalse();
  }
}
