package com.vocaloidarchive.character.application;

import com.vocaloidarchive.character.application.dto.result.CharacterSummaryResult;
import com.vocaloidarchive.character.application.port.CharacterQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCharactersUseCase {

  private final CharacterQueryRepository characterQueryRepository;

  @Transactional(readOnly = true)
  public List<CharacterSummaryResult> invoke() {
    return characterQueryRepository.findAllSummariesOrderByIdAsc();
  }
}
