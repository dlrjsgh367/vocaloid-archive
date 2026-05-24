package com.vocaloidarchive.share.application;

import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.share.application.dto.CardRenderRequest;
import com.vocaloidarchive.share.application.port.CardCachePort;
import com.vocaloidarchive.share.application.port.CardRendererPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resolves a playlist's card PNG by share code: serves a cached render when the playlist content is
 * unchanged, otherwise renders fresh and caches it. Access control (public-only) is delegated to
 * {@link GetPlaylistCardDataUseCase}.
 */
@Service
@RequiredArgsConstructor
public class RenderPlaylistCardUseCase {

  private static final int COLLAGE_SIZE = 4;
  private static final int TRACKLIST_SIZE = 3;

  private final GetPlaylistCardDataUseCase getCardData;
  private final CardRendererPort renderer;
  private final CardCachePort cache;

  public byte[] invoke(String shareCode) {
    PlaylistCardData data = getCardData.invoke(shareCode); // throws if missing/private
    String hash = contentHash(data);

    Optional<byte[]> cached = cache.get(shareCode, hash);
    if (cached.isPresent()) {
      return cached.get();
    }
    byte[] png = renderer.render(toRequest(data));
    cache.put(shareCode, hash, png);
    return png;
  }

  private CardRenderRequest toRequest(PlaylistCardData d) {
    var collage = d.songs().stream()
        .limit(COLLAGE_SIZE)
        .map(s -> new CardRenderRequest.Tile(s.title(), s.thumbnailUrl()))
        .toList();
    var tracklist = d.songs().stream()
        .limit(TRACKLIST_SIZE)
        .map(s -> new CardRenderRequest.Track(s.title(), s.mood()))
        .toList();
    String shareUrl = "voca.archive/p/" + d.shareCode();
    return new CardRenderRequest(d.title(), d.ownerUsername(), d.songCount(), d.likeSum(),
        shareUrl, d.themeColorHex(), d.primaryCharName(), collage, tracklist);
  }

  /** Stable hash of everything that affects the rendered pixels. */
  private String contentHash(PlaylistCardData d) {
    StringBuilder sb = new StringBuilder()
        .append(d.title()).append('|')
        .append(d.isPublic()).append('|')
        .append(d.themeColorHex()).append('|')
        .append(d.primaryCharName()).append('|')
        .append(d.likeSum());
    for (PlaylistCardData.SongLine s : d.songs()) {
      sb.append('|').append(s.songId()).append(',').append(s.thumbnailUrl()).append(',').append(s.mood());
    }
    return sha256Hex(sb.toString());
  }

  private static String sha256Hex(String input) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
