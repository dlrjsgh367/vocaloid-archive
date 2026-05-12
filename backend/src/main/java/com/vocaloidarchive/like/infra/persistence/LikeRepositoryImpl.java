package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.like.application.port.LikeRepository;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

  private final LikeJpaRepository jpa;
  private final UserJpaRepository userJpa;
  private final SongRepository songJpa;

  @Override
  public boolean existsBy(Long userId, Long songId) {
    return jpa.existsById(new LikeId(userId, songId));
  }

  @Override
  public Like save(Like like) {
    UserEntity userRef = userJpa.getReferenceById(like.getUserId());
    Song songRef = songJpa.getReferenceById(like.getSongId());
    LikeEntity saved = jpa.save(LikeEntity.of(userRef, songRef));
    return LikeEntityMapper.toDomain(saved);
  }

  @Override
  public void deleteBy(Long userId, Long songId) {
    jpa.deleteById(new LikeId(userId, songId));
  }
}
