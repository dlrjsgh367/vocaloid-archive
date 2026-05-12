package com.vocaloidarchive.like.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeJpaRepository extends JpaRepository<LikeEntity, LikeId> {
  long countBySongId(Long songId);
}
