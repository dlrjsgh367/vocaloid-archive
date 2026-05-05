package com.vocaloidarchive.tag.repository;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import com.vocaloidarchive.tag.domain.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TagRepositoryTest extends AbstractMysqlContainerTest {

  @Autowired TagRepository tagRepository;

  @Test
  @DisplayName("findAllByNameIn: 매치되는 태그만 반환")
  void findAllByNameIn_matches() {
    tagRepository.saveAll(List.of(Tag.of("pop"), Tag.of("rock"), Tag.of("ballad")));

    List<Tag> result = tagRepository.findAllByNameIn(List.of("pop", "ballad", "missing"));

    assertThat(result).extracting(Tag::getName).containsExactlyInAnyOrder("pop", "ballad");
  }

  @Test
  @DisplayName("existsByName: 정확 일치 시 true")
  void existsByName() {
    tagRepository.save(Tag.of("anime"));

    assertThat(tagRepository.existsByName("anime")).isTrue();
    assertThat(tagRepository.existsByName("Anime")).isFalse();
    assertThat(tagRepository.existsByName("missing")).isFalse();
  }
}
