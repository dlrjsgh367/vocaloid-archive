package com.vocaloidarchive.stats.interfaces.dto.response;

import com.vocaloidarchive.stats.application.dto.result.StatsResult;

public record StatsResponse(long songCount, long userCount, long tagCount) {
  public static StatsResponse from(StatsResult r) {
    return new StatsResponse(r.songCount(), r.userCount(), r.tagCount());
  }
}
