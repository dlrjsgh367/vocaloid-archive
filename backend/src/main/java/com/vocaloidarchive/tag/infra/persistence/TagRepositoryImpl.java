package com.vocaloidarchive.tag.infra.persistence;

import com.vocaloidarchive.tag.application.port.TagRepository;
import com.vocaloidarchive.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

  private final TagJpaRepository jpa;

  @Override
  public List<Tag> findAllByNameIn(Collection<String> names) {
    return jpa.findAllByNameIn(names).stream()
        .map(TagEntityMapper::toDomain)
        .toList();
  }

  @Override
  public List<Tag> saveAll(List<Tag> tags) {
    List<TagEntity> entities = tags.stream().map(TagEntityMapper::toNewEntity).toList();
    return jpa.saveAll(entities).stream().map(TagEntityMapper::toDomain).toList();
  }
}
