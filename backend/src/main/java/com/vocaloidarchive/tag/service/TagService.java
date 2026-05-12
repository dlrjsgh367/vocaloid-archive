package com.vocaloidarchive.tag.service;

import com.vocaloidarchive.tag.infra.persistence.TagEntity;
import com.vocaloidarchive.tag.infra.persistence.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

  private final TagJpaRepository tagRepository;

  @Transactional
  public List<TagEntity> findOrCreateAll(List<String> rawNames) {
    if (rawNames == null || rawNames.isEmpty()) return List.of();

    Set<String> normalized = new LinkedHashSet<>();
    for (String raw : rawNames) {
      if (raw == null) continue;
      String n = raw.trim().toLowerCase();
      if (!n.isEmpty()) normalized.add(n);
    }
    if (normalized.isEmpty()) return List.of();

    Map<String, TagEntity> existing = tagRepository.findAllByNameIn(normalized).stream()
        .collect(Collectors.toMap(TagEntity::getName, Function.identity()));

    List<TagEntity> toCreate = new ArrayList<>();
    for (String n : normalized) {
      if (!existing.containsKey(n)) toCreate.add(TagEntity.of(n));
    }
    if (!toCreate.isEmpty()) {
      try {
        tagRepository.saveAll(toCreate);
      } catch (DataIntegrityViolationException ex) {
        // Concurrent insert won the race — re-read
      }
      tagRepository.findAllByNameIn(normalized).forEach(t -> existing.put(t.getName(), t));
    }

    List<TagEntity> result = new ArrayList<>(normalized.size());
    for (String n : normalized) result.add(existing.get(n));
    return result;
  }
}
