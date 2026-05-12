package com.vocaloidarchive.tag.application.dto.result;

import com.vocaloidarchive.tag.domain.Tag;

public record TagResult(Long id, String name) {
  public static TagResult from(Tag t) {
    return new TagResult(t.getId(), t.getName());
  }
}
