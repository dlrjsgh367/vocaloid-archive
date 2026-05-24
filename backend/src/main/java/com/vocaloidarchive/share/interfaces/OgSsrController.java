package com.vocaloidarchive.share.interfaces;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Server-rendered Open Graph page for share links. nginx routes only crawler User-Agents here
 * ({@code GET /share/p/{code}}); humans get the SPA. Outputs minimal HTML with OG/Twitter meta so
 * SNS link previews show the playlist card.
 */
@RestController
@RequiredArgsConstructor
public class OgSsrController {

  private final GetPlaylistCardDataUseCase getCardData;

  @GetMapping(value = "/share/p/{code}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> page(@PathVariable String code) {
    PlaylistCardData data;
    try {
      data = getCardData.invoke(code);
    } catch (BusinessException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(MediaType.TEXT_HTML)
          .body(notFoundHtml());
    }

    String base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    String imageUrl = base + "/api/share/playlists/" + code + "/card.png";
    String pageUrl = base + "/p/" + code;
    String title = esc(data.title()) + " · VocaloidArchive";
    String desc = esc(data.ownerUsername() + "의 보카로 플레이리스트 · " + data.songCount() + "곡");

    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(ogHtml(title, desc, imageUrl, pageUrl));
  }

  private String ogHtml(String title, String desc, String imageUrl, String pageUrl) {
    return """
        <!doctype html><html lang="ko"><head><meta charset="utf-8"/>
        <title>%s</title>
        <meta property="og:type" content="website"/>
        <meta property="og:title" content="%s"/>
        <meta property="og:description" content="%s"/>
        <meta property="og:image" content="%s"/>
        <meta property="og:url" content="%s"/>
        <meta name="twitter:card" content="summary_large_image"/>
        <meta name="twitter:title" content="%s"/>
        <meta name="twitter:image" content="%s"/>
        <link rel="canonical" href="%s"/>
        <meta http-equiv="refresh" content="0; url=%s"/>
        </head><body><a href="%s">%s</a></body></html>
        """.formatted(title, title, desc, imageUrl, pageUrl, title, imageUrl, pageUrl, pageUrl, pageUrl, title);
  }

  private String notFoundHtml() {
    return "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\"/>"
        + "<title>VocaloidArchive</title></head><body>플레이리스트를 찾을 수 없습니다.</body></html>";
  }

  private String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
