package com.vocaloidarchive.tag.application.port;

import com.vocaloidarchive.tag.domain.Tag;
import java.util.Collection;
import java.util.List;

public interface TagRepository {
  List<Tag> findAllByNameIn(Collection<String> names);
  List<Tag> saveAll(List<Tag> tags);
}
