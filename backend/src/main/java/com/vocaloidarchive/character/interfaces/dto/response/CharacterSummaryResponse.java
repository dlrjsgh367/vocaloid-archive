package com.vocaloidarchive.character.interfaces.dto.response;

import com.vocaloidarchive.character.application.dto.result.CharacterSummaryResult;

public record CharacterSummaryResponse(
    Long id, String name, String colorHex, String imageUrl, long songCount) {

  public static CharacterSummaryResponse from(CharacterSummaryResult r) {
    return new CharacterSummaryResponse(
        r.id(), r.name(), r.colorHex(), r.imageUrl(), r.songCount());
  }
}
