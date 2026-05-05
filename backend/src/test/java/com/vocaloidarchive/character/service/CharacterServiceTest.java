package com.vocaloidarchive.character.service;

import com.vocaloidarchive.character.domain.Character;
import com.vocaloidarchive.character.dto.response.CharacterResponse;
import com.vocaloidarchive.character.repository.CharacterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

  @Mock CharacterRepository characterRepository;
  @InjectMocks CharacterService characterService;

  @Test
  @DisplayName("findAll: id 오름차순으로 매핑해 반환")
  void findAll_maps_in_order() {
    given(characterRepository.findAllByOrderByIdAsc())
        .willReturn(List.of(
            Character.of("하츠네 미쿠", "#39C5BB", null),
            Character.of("KAITO", "#1E90FF", null)));

    List<CharacterResponse> result = characterService.findAll();

    assertThat(result).extracting(CharacterResponse::name)
        .containsExactly("하츠네 미쿠", "KAITO");
  }
}
