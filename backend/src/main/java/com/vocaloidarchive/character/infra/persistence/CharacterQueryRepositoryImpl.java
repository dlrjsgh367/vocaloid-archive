package com.vocaloidarchive.character.infra.persistence;

import com.vocaloidarchive.character.application.dto.result.CharacterResult;
import com.vocaloidarchive.character.application.port.CharacterQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CharacterQueryRepositoryImpl implements CharacterQueryRepository {

  private final CharacterJpaRepository jpa;

  @Override
  public List<CharacterResult> findAllOrderByIdAsc() {
    return jpa.findAllByOrderByIdAsc().stream()
        .map(e -> new CharacterResult(e.getId(), e.getName(), e.getColorHex(), e.getImageUrl()))
        .toList();
  }
}
