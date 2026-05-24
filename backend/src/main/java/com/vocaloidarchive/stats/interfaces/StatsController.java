package com.vocaloidarchive.stats.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.stats.application.GetStatsUseCase;
import com.vocaloidarchive.stats.interfaces.dto.response.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

  private final GetStatsUseCase getStatsUseCase;

  @GetMapping
  public ApiResponse<StatsResponse> get() {
    return ApiResponse.success(StatsResponse.from(getStatsUseCase.invoke()));
  }
}
