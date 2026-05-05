package com.vocaloidarchive.tag.service;

import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.tag.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

  @Mock TagRepository tagRepository;
  @InjectMocks TagService tagService;

  @Test
  @DisplayName("findOrCreateAll: 정규화 후 중복 제거하여 한 번만 조회/생성")
  void normalize_and_dedupe() {
    given(tagRepository.findAllByNameIn(anyCollection()))
        .willReturn(List.of())
        .willReturn(List.of(Tag.of("pop")));

    List<Tag> result = tagService.findOrCreateAll(List.of("  Pop  ", "POP", "pop"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("pop");
    then(tagRepository).should().saveAll(any());
  }

  @Test
  @DisplayName("findOrCreateAll: 기존 + 신규 mix → 신규만 saveAll")
  void mix_existing_and_new() {
    given(tagRepository.findAllByNameIn(anyCollection()))
        .willReturn(List.of(Tag.of("rock")))
        .willReturn(List.of(Tag.of("rock"), Tag.of("ballad")));

    List<Tag> result = tagService.findOrCreateAll(List.of("rock", "ballad"));

    assertThat(result).extracting(Tag::getName).containsExactly("rock", "ballad");
    then(tagRepository).should().saveAll(any());
  }

  @Test
  @DisplayName("findOrCreateAll: 모두 기존이면 saveAll 호출 안 함")
  void all_existing_no_save() {
    given(tagRepository.findAllByNameIn(anyCollection()))
        .willReturn(List.of(Tag.of("pop"), Tag.of("rock")));

    List<Tag> result = tagService.findOrCreateAll(List.of("pop", "rock"));

    assertThat(result).hasSize(2);
    then(tagRepository).should(org.mockito.Mockito.never()).saveAll(any());
  }

  @Test
  @DisplayName("findOrCreateAll: null 입력 → 빈 리스트")
  void null_input_returns_empty() {
    assertThat(tagService.findOrCreateAll(null)).isEmpty();
    assertThat(tagService.findOrCreateAll(List.of())).isEmpty();
    assertThat(tagService.findOrCreateAll(List.of("   ", ""))).isEmpty();
  }
}
