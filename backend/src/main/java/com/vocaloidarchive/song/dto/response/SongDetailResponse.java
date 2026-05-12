package com.vocaloidarchive.song.dto.response;

import com.vocaloidarchive.character.interfaces.dto.response.CharacterResponse;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;

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
    LocalDateTime createdAt
) {
  public static SongDetailResponse from(Song s, long likeCount) {
    return new SongDetailResponse(
        s.getId(),
        s.getTitle(),
        s.getYoutubeUrl(),
        s.getNiconicoUrl(),
        s.getThumbnailUrl(),
        s.getBpm(),
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
