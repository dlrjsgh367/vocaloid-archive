package com.vocaloidarchive.song.application.dto.result;

import com.vocaloidarchive.song.domain.Mood;
import java.time.LocalDateTime;
import java.util.List;

public record SongResult(
    Long id,
    String title,
    String thumbnailUrl,
    Mood mood,
    Integer playCount,
    Long likeCount,
    Owner registeredBy,
    List<CharacterRef> characters,
    List<String> tags,
    LocalDateTime createdAt) {

  public record Owner(Long id, String username) {}

  public record CharacterRef(Long id, String name, String colorHex, String imageUrl) {}
}
