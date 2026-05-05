package com.vocaloidarchive.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeUtilTest {

  private static final String VID = "dQw4w9WgXcQ";
  private static final String EXPECTED = "https://i.ytimg.com/vi/" + VID + "/hqdefault.jpg";

  @Test
  @DisplayName("youtu.be 단축 URL")
  void shortUrl() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://youtu.be/" + VID)).isEqualTo(EXPECTED);
  }

  @Test
  @DisplayName("watch?v= URL")
  void watchUrl() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://www.youtube.com/watch?v=" + VID))
        .isEqualTo(EXPECTED);
  }

  @Test
  @DisplayName("watch?v= + 추가 쿼리")
  void watchUrlWithExtraQuery() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://www.youtube.com/watch?v=" + VID + "&t=10s"))
        .isEqualTo(EXPECTED);
  }

  @Test
  @DisplayName("embed URL")
  void embedUrl() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://www.youtube.com/embed/" + VID))
        .isEqualTo(EXPECTED);
  }

  @Test
  @DisplayName("shorts URL")
  void shortsUrl() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://www.youtube.com/shorts/" + VID))
        .isEqualTo(EXPECTED);
  }

  @Test
  @DisplayName("YouTube 도메인 아닌 URL → null")
  void nonYoutubeUrl() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://example.com/" + VID)).isNull();
  }

  @Test
  @DisplayName("videoId 11자 미만 → null")
  void shortVideoId() {
    assertThat(YoutubeUtil.extractThumbnailUrl("https://www.youtube.com/watch?v=abc")).isNull();
  }

  @Test
  @DisplayName("null 입력 → null")
  void nullInput() {
    assertThat(YoutubeUtil.extractThumbnailUrl(null)).isNull();
  }
}
