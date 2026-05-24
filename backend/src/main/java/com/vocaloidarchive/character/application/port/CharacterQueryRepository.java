package com.vocaloidarchive.character.application.port;

import com.vocaloidarchive.character.application.dto.result.CharacterResult;
import com.vocaloidarchive.character.application.dto.result.CharacterSummaryResult;
import java.util.List;

public interface CharacterQueryRepository {
  List<CharacterResult> findAllOrderByIdAsc();

  List<CharacterSummaryResult> findAllSummariesOrderByIdAsc();
}
