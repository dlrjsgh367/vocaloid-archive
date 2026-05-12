package com.vocaloidarchive.character.dto.response;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;

public record CharacterResponse(
    Long id,
    String name,
    String colorHex,
    String imageUrl
) {
  public static CharacterResponse from(CharacterEntity c) {
    return new CharacterResponse(c.getId(), c.getName(), c.getColorHex(), c.getImageUrl());
  }
}
