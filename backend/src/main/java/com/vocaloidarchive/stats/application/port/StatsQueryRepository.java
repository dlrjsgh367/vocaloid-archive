package com.vocaloidarchive.stats.application.port;

import com.vocaloidarchive.stats.application.dto.result.StatsResult;

public interface StatsQueryRepository {
  StatsResult getStats();
}
