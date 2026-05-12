package com.vocaloidarchive.song.dto.response;

import com.vocaloidarchive.character.interfaces.dto.response.CharacterResponse;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.infra.persistence.SongEntity;

import java.time.LocalDateTime;
import java.util.List;

public record SongResponse(
    Long id,
    String title,
    String thumbnailUrl,
    Mood mood,
    Integer playCount,
    Long likeCount,
    UserSummary registeredBy,
    List<CharacterResponse> characters,
    List<String> tags,
    LocalDateTime createdAt
) {
  public static SongResponse from(SongEntity s, long likeCount) {
    return new SongResponse(
        s.getId(),
        s.getTitle(),
        s.getThumbnailUrl(),
        s.getMood(),
        s.getPlayCount(),
        likeCount,
        UserSummary.from(s.getRegisteredBy()),
        s.getCharacters().stream()
            .map(sc -> CharacterResponse.from(sc.getCharacter()))
            .toList(),
        s.getTags().stream()
            .map(st -> st.getTag().getName())
            .toList(),
        s.getCreatedAt());
  }
}
