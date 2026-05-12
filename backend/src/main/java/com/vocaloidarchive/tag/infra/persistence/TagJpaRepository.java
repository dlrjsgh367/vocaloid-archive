package com.vocaloidarchive.tag.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagJpaRepository extends JpaRepository<TagEntity, Long> {
  List<TagEntity> findAllByNameIn(Collection<String> names);
  boolean existsByName(String name);
}
