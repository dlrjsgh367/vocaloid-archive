package com.vocaloidarchive.comment.infra.persistence;

import com.vocaloidarchive.comment.application.port.CommentRepository;
import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepository {
  private final CommentJpaRepository jpa;
  private final UserJpaRepository userJpa;
  private final SongRepository songJpa;

  @Override
  public Comment save(Comment c) {
    UserEntity userRef = userJpa.getReferenceById(c.getUserId());
    Song songRef = songJpa.getReferenceById(c.getSongId());
    CommentEntity saved = jpa.save(CommentEntity.of(userRef, songRef, c.getContent()));
    return CommentEntityMapper.toDomain(saved);
  }

  @Override
  public Optional<Long> findUserIdById(Long id) {
    return jpa.findById(id).map(e -> e.getUser().getId());
  }

  @Override
  public boolean existsById(Long id) {
    return jpa.existsById(id);
  }

  @Override
  public void deleteById(Long id) {
    jpa.deleteById(id);
  }
}
