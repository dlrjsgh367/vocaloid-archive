package com.vocaloidarchive.song.application.port;

import com.vocaloidarchive.song.domain.Song;
import java.util.List;
import java.util.Optional;

public interface SongRepository {
  Song save(Song song, List<Long> characterIds, List<Long> tagIds);
  Optional<Song> findById(Long id);
  boolean existsById(Long id);
  int incrementPlayCount(Long id);
  void deleteById(Long id);
}
