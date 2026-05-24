package com.vocaloidarchive.share.interfaces.dto;

import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import java.util.List;

/** Public-facing card data for the SPA share page. Omits internal flags like {@code isPublic}. */
public record ShareCardDataResponse(
    String shareCode,
    String title,
    String ownerUsername,
    int songCount,
    long likeSum,
    String themeColorHex,
    String primaryCharName,
    List<SongItem> songs) {

  public record SongItem(Long songId, String title, String thumbnailUrl, String mood) {}

  public static ShareCardDataResponse from(PlaylistCardData d) {
    List<SongItem> songs = d.songs().stream()
        .map(s -> new SongItem(s.songId(), s.title(), s.thumbnailUrl(), s.mood()))
        .toList();
    return new ShareCardDataResponse(d.shareCode(), d.title(), d.ownerUsername(), d.songCount(),
        d.likeSum(), d.themeColorHex(), d.primaryCharName(), songs);
  }
}
