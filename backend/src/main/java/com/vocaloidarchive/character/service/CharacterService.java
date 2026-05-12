package com.vocaloidarchive.character.service;

import com.vocaloidarchive.character.dto.response.CharacterResponse;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

  private final CharacterJpaRepository characterRepository;

  public List<CharacterResponse> findAll() {
    return characterRepository.findAllByOrderByIdAsc().stream()
        .map(CharacterResponse::from)
        .toList();
  }
}
