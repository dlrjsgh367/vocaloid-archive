package com.vocaloidarchive.playlist.application.dto.result;

import java.util.List;

/**
 * Everything needed to render a playlist share card. Songs are returned in order; the consumer
 * slices the top 4 for the collage and top 3 for the tracklist. {@code themeColorHex} is the
 * most-frequent character's color (defaults to Miku teal when the playlist has no characters).
 */
public record PlaylistCardData(
    Long id,
    String shareCode,
    String title,
    String ownerUsername,
    int songCount,
    long likeSum,
    String themeColorHex,
    String primaryCharName,
    boolean isPublic,
    List<SongLine> songs) {

  public record SongLine(Long songId, String title, String thumbnailUrl, String mood) {}

  public static final String DEFAULT_THEME_HEX = "#39C5BB"; // Hatsune Miku
}
