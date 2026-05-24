package com.vocaloidarchive.share.application.dto;

import java.util.List;

/** Exact JSON payload sent to the renderer service ({@code POST /render}). */
public record CardRenderRequest(
    String title,
    String ownerUsername,
    int songCount,
    long likeSum,
    String shareUrl,
    String themeColorHex,
    String primaryCharName,
    List<Tile> collage,
    List<Track> tracklist) {

  public record Tile(String title, String thumbnailUrl) {}

  public record Track(String title, String mood) {}
}
