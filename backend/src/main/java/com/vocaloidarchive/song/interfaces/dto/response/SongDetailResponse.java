package com.vocaloidarchive.song.interfaces.dto.response;

import com.vocaloidarchive.character.interfaces.dto.response.CharacterResponse;
import com.vocaloidarchive.song.application.dto.result.SongDetailResult;
import com.vocaloidarchive.song.domain.Mood;
import java.time.LocalDateTime;
import java.util.List;

public record SongDetailResponse(
    Long id,
    String title,
    String youtubeUrl,
    String niconicoUrl,
    String thumbnailUrl,
    Integer bpm,
    Mood mood,
    Integer playCount,
    Long likeCount,
    UserSummary registeredBy,
    List<CharacterResponse> characters,
    List<String> tags,
    LocalDateTime createdAt) {

  public static SongDetailResponse from(SongDetailResult r) {
    return new SongDetailResponse(
        r.id(),
        r.title(),
        r.youtubeUrl(),
        r.niconicoUrl(),
        r.thumbnailUrl(),
        r.bpm(),
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
