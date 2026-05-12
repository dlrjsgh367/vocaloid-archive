package com.vocaloidarchive.user.application.port;

import com.vocaloidarchive.user.application.dto.result.UserSummaryResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserQueryRepository {
  Optional<UserSummaryResult> findSummaryById(Long id);
  List<UserSummaryResult> findSummariesByIds(Set<Long> ids);
}
