package com.vocaloidarchive.tag.infra.persistence;

import com.vocaloidarchive.tag.domain.Tag;

final class TagEntityMapper {

  private TagEntityMapper() {}

  static Tag toDomain(TagEntity e) {
    return e == null ? null : Tag.reconstitute(e.getId(), e.getName());
  }

  static TagEntity toNewEntity(Tag t) {
    return TagEntity.of(t.getName());
  }
}
