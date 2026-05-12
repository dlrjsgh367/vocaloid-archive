package com.vocaloidarchive.song.infra.persistence;

import com.vocaloidarchive.song.domain.Song;
import org.springframework.stereotype.Component;

@Component
public class SongEntityMapper {

  public Song toDomain(SongEntity entity) {
    return Song.reconstitute(
        entity.getId(),
        entity.getRegisteredBy().getId(),
        entity.getTitle(),
        entity.getYoutubeUrl(),
        entity.getNiconicoUrl(),
        entity.getThumbnailUrl(),
        entity.getBpm(),
        entity.getMood(),
        entity.getPlayCount(),
        entity.getCreatedAt());
  }
}
