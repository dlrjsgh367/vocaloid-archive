package com.vocaloidarchive.share.application.port;

import java.util.Optional;

/** Caches rendered card PNGs keyed by share code + content hash. */
public interface CardCachePort {
  Optional<byte[]> get(String shareCode, String contentHash);

  void put(String shareCode, String contentHash, byte[] png);
}
