package com.vocaloidarchive.stats.infra.persistence;

import com.vocaloidarchive.stats.application.dto.result.StatsResult;
import com.vocaloidarchive.stats.application.port.StatsQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsQueryRepositoryImpl implements StatsQueryRepository {

  private final EntityManager em;

  @Override
  public StatsResult getStats() {
    long songCount = count("com.vocaloidarchive.song.infra.persistence.SongEntity");
    long userCount = count("com.vocaloidarchive.user.infra.persistence.UserEntity");
    long tagCount = count("com.vocaloidarchive.tag.infra.persistence.TagEntity");
    return new StatsResult(songCount, userCount, tagCount);
  }

  private long count(String entityFqn) {
    return em.createQuery("SELECT COUNT(e) FROM " + entityFqn + " e", Long.class)
        .getSingleResult();
  }
}
