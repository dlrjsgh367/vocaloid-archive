package com.vocaloidarchive.song.interfaces.dto.response;

import com.vocaloidarchive.character.interfaces.dto.response.CharacterResponse;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import com.vocaloidarchive.song.domain.Mood;
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
    LocalDateTime createdAt) {

  public static SongResponse from(SongResult r) {
    return new SongResponse(
        r.id(),
        r.title(),
        r.thumbnailUrl(),
        r.mood(),
        r.playCount(),
        r.likeCount(),
        r.registeredBy() != null ? UserSummary.from(r.registeredBy()) : null,
        r.characters() != null
            ? r.characters().stream()
                .map(c -> new CharacterResponse(c.id(), c.name(), c.colorHex(), c.imageUrl()))
                .toList()
            : List.of(),
        r.tags(),
        r.createdAt());
  }
}
