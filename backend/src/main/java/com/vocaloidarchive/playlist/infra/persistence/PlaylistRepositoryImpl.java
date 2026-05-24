package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements PlaylistRepository {
  private final PlaylistJpaRepository jpa;
  private final UserJpaRepository userJpa;
  private final PlaylistDomainMapper mapper;

  @Override
  public Playlist save(Playlist p) {
    UserEntity userRef = userJpa.getReferenceById(p.getUserId());
    PlaylistEntity saved = jpa.save(PlaylistEntity.of(userRef, p.getTitle(), p.isPublic()));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Playlist> findById(Long id) {
    return jpa.findWithUserById(id).map(mapper::toDomain);
  }

  @Override
  public void deleteById(Long id) {
    jpa.deleteById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return jpa.existsById(id);
  }

  @Override
  public boolean existsByShareCode(String shareCode) {
    return jpa.existsByShareCode(shareCode);
  }

  @Override
  public void updateShareCode(Long playlistId, String shareCode) {
    PlaylistEntity e = jpa.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    e.assignShareCode(shareCode);
    jpa.saveAndFlush(e);
  }
}
