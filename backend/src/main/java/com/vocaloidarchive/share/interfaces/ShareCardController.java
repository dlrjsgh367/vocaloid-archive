package com.vocaloidarchive.share.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.playlist.application.EnsureShareCodeUseCase;
import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.share.application.RenderPlaylistCardUseCase;
import com.vocaloidarchive.share.interfaces.dto.ShareCardDataResponse;
import com.vocaloidarchive.share.interfaces.dto.ShareResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class ShareCardController {

  private final EnsureShareCodeUseCase ensureShareCode;
  private final GetPlaylistCardDataUseCase getCardData;
  private final RenderPlaylistCardUseCase renderCard;

  /** Owner-only: issue (or return) the playlist's share code + absolute share URL. */
  @PostMapping("/api/playlists/{id}/share")
  public ResponseEntity<ApiResponse<ShareResponse>> share(@PathVariable Long id) {
    String code = ensureShareCode.invoke(id);
    String shareUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/p/").path(code).toUriString();
    return ResponseEntity.ok(ApiResponse.success(new ShareResponse(code, shareUrl)));
  }

  /** Public: card data for the SPA share page (public playlists only). */
  @GetMapping("/api/share/playlists/{code}")
  public ResponseEntity<ApiResponse<ShareCardDataResponse>> data(@PathVariable String code) {
    return ResponseEntity.ok(
        ApiResponse.success(ShareCardDataResponse.from(getCardData.invoke(code))));
  }

  /** Public: rendered 1080x1080 PNG card (used as og:image and for download). */
  @GetMapping("/api/share/playlists/{code}/card.png")
  public ResponseEntity<byte[]> cardPng(@PathVariable String code) {
    byte[] png = renderCard.invoke(code);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
        .body(png);
  }
}
