package com.vocaloidarchive.song.application.dto.result;

import com.vocaloidarchive.song.domain.Mood;
import java.time.LocalDateTime;
import java.util.List;

public record SongDetailResult(
    Long id,
    String title,
    String youtubeUrl,
    String niconicoUrl,
    String thumbnailUrl,
    Integer bpm,
    Mood mood,
    Integer playCount,
    Long likeCount,
    SongResult.Owner registeredBy,
    List<SongResult.CharacterRef> characters,
    List<String> tags,
    LocalDateTime createdAt) {}
