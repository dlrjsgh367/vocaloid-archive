package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.like.application.port.LikeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeQueryRepositoryImpl implements LikeQueryRepository {

  private final LikeJpaRepository jpa;

  @Override
  public long countBySongId(Long songId) {
    return jpa.countBySongId(songId);
  }
}
