package com.vocaloidarchive.stats.application;

import com.vocaloidarchive.stats.application.dto.result.StatsResult;
import com.vocaloidarchive.stats.application.port.StatsQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetStatsUseCase {

  private final StatsQueryRepository statsQueryRepository;

  @Transactional(readOnly = true)
  public StatsResult invoke() {
    return statsQueryRepository.getStats();
  }
}
