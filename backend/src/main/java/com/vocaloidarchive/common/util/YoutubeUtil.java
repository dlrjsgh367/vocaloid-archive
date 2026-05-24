package com.vocaloidarchive.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeUtil {

  private static final Pattern PATTERN = Pattern.compile(
      "(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/))([A-Za-z0-9_-]{11})",
      Pattern.CASE_INSENSITIVE);

  private YoutubeUtil() {}

  public static String extractThumbnailUrl(String url) {
    if (url == null) return null;
    Matcher m = PATTERN.matcher(url);
    return m.find() ? "https://i.ytimg.com/vi/" + m.group(1) + "/hqdefault.jpg" : null;
  }

  /**
   * Resolves the thumbnail to display: the stored value if present, otherwise the YouTube
   * thumbnail derived from the YouTube URL. Returns null when neither is available, leaving the
   * client to render an empty-image placeholder.
   */
  public static String resolveThumbnailUrl(String storedThumbnailUrl, String youtubeUrl) {
    if (storedThumbnailUrl != null && !storedThumbnailUrl.isBlank()) {
      return storedThumbnailUrl;
    }
    return extractThumbnailUrl(youtubeUrl);
  }
}
