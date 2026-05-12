package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.dto.result.UserSummaryResult;
import com.vocaloidarchive.user.application.port.UserQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

  private final UserJpaRepository jpa;

  @Override
  public Optional<UserSummaryResult> findSummaryById(Long id) {
    return jpa.findById(id).map(e -> new UserSummaryResult(e.getId(), e.getUsername()));
  }

  @Override
  public List<UserSummaryResult> findSummariesByIds(Set<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return jpa.findAllById(ids).stream()
        .map(e -> new UserSummaryResult(e.getId(), e.getUsername()))
        .toList();
  }
}
