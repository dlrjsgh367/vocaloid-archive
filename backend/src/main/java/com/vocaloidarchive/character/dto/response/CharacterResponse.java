package com.vocaloidarchive.character.dto.response;

import com.vocaloidarchive.character.domain.Character;

public record CharacterResponse(
    Long id,
    String name,
    String colorHex,
    String imageUrl
) {
  public static CharacterResponse from(Character c) {
    return new CharacterResponse(c.getId(), c.getName(), c.getColorHex(), c.getImageUrl());
  }
}
