package com.vocaloidarchive.like.repository;

import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
  long countBySongId(Long songId);
}
