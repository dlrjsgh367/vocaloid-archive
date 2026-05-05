package com.vocaloidarchive.tag.service;

import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.tag.repository.TagRepository;
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

  private final TagRepository tagRepository;

  @Transactional
  public List<Tag> findOrCreateAll(List<String> rawNames) {
    if (rawNames == null || rawNames.isEmpty()) return List.of();

    Set<String> normalized = new LinkedHashSet<>();
    for (String raw : rawNames) {
      if (raw == null) continue;
      String n = raw.trim().toLowerCase();
      if (!n.isEmpty()) normalized.add(n);
    }
    if (normalized.isEmpty()) return List.of();

    Map<String, Tag> existing = tagRepository.findAllByNameIn(normalized).stream()
        .collect(Collectors.toMap(Tag::getName, Function.identity()));

    List<Tag> toCreate = new ArrayList<>();
    for (String n : normalized) {
      if (!existing.containsKey(n)) toCreate.add(Tag.of(n));
    }
    if (!toCreate.isEmpty()) {
      try {
        tagRepository.saveAll(toCreate);
      } catch (DataIntegrityViolationException ex) {
        // Concurrent insert won the race — re-read
      }
      tagRepository.findAllByNameIn(normalized).forEach(t -> existing.put(t.getName(), t));
    }

    List<Tag> result = new ArrayList<>(normalized.size());
    for (String n : normalized) result.add(existing.get(n));
    return result;
  }
}
