package com.vocaloidarchive.tag.application;

import com.vocaloidarchive.tag.application.dto.result.TagResult;
import com.vocaloidarchive.tag.application.port.TagRepository;
import com.vocaloidarchive.tag.domain.Tag;
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
public class FindOrCreateTagsUseCase {

  private final TagRepository tagRepository;

  @Transactional
  public List<TagResult> invoke(List<String> rawNames) {
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
      if (!existing.containsKey(n)) toCreate.add(Tag.newTag(n));
    }
    if (!toCreate.isEmpty()) {
      try {
        tagRepository.saveAll(toCreate);
      } catch (DataIntegrityViolationException ex) {
        // Concurrent insert won the race — re-read
      }
      tagRepository.findAllByNameIn(normalized).forEach(t -> existing.put(t.getName(), t));
    }

    return normalized.stream().map(existing::get).map(TagResult::from).toList();
  }
}
