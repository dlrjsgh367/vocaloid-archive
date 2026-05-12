package com.vocaloidarchive.song.application.dto.command;

import com.vocaloidarchive.song.domain.Mood;
import java.util.List;

public record CreateSongCommand(
    Long userId,
    String title,
    String youtubeUrl,
    String niconicoUrl,
    Integer bpm,
    Mood mood,
    List<Long> characterIds,
    List<String> tagNames) {}
