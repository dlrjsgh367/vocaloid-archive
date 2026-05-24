package com.vocaloidarchive.share.infra;

import com.vocaloidarchive.share.application.port.CardCachePort;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Filesystem cache for rendered cards. Keyed by {@code {shareCode}_{contentHash}.png}; a content
 * change yields a new hash, so stale files simply stop being requested (TTL cleanup is backlog).
 * All IO failures degrade gracefully — a cache miss just triggers a re-render.
 */
@Component
public class FileCardCache implements CardCachePort {

  private final Path dir;

  public FileCardCache(@Value("${CARD_CACHE_DIR:/var/cache/cards}") String dir) {
    this.dir = Path.of(dir);
  }

  @Override
  public Optional<byte[]> get(String shareCode, String contentHash) {
    Path file = fileFor(shareCode, contentHash);
    if (!Files.isReadable(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readAllBytes(file));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  @Override
  public void put(String shareCode, String contentHash, byte[] png) {
    try {
      Files.createDirectories(dir);
      Files.write(fileFor(shareCode, contentHash), png);
    } catch (IOException e) {
      // best-effort cache; ignore write failures
    }
  }

  private Path fileFor(String shareCode, String contentHash) {
    return dir.resolve(shareCode + "_" + contentHash + ".png");
  }
}
