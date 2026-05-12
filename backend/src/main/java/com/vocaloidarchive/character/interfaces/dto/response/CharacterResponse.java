package com.vocaloidarchive.character.interfaces.dto.response;

import com.vocaloidarchive.character.application.dto.result.CharacterResult;
import com.vocaloidarchive.character.infra.persistence.CharacterEntity;

public record CharacterResponse(Long id, String name, String colorHex, String imageUrl) {
  public static CharacterResponse from(CharacterResult r) {
    return new CharacterResponse(r.id(), r.name(), r.colorHex(), r.imageUrl());
  }

  public static CharacterResponse from(CharacterEntity c) {
    return new CharacterResponse(c.getId(), c.getName(), c.getColorHex(), c.getImageUrl());
  }
}
