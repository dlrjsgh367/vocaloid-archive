package com.vocaloidarchive.song.infra.persistence;

import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.song.application.port.SongRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.tag.infra.persistence.TagJpaRepository;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class SongRepositoryImpl implements SongRepository {

  private final SongJpaRepository songJpa;
  private final UserJpaRepository userJpa;
  private final CharacterJpaRepository characterJpa;
  private final TagJpaRepository tagJpa;
  private final SongDomainMapper mapper;

  @Override
  @Transactional
  public Song save(Song song, List<Long> characterIds, List<Long> tagIds) {
    SongEntity entity = SongEntity.of(
        userJpa.getReferenceById(song.getRegisteredById()),
        song.getTitle(),
        song.getYoutubeUrl(),
        song.getNiconicoUrl(),
        song.getThumbnailUrl(),
        song.getBpm(),
        song.getMood());

    characterIds.forEach(cid -> entity.addCharacter(characterJpa.getReferenceById(cid)));
    tagIds.forEach(tid -> entity.addTag(tagJpa.getReferenceById(tid)));

    SongEntity saved = songJpa.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Song> findById(Long id) {
    return songJpa.findById(id).map(mapper::toDomain);
  }

  @Override
  public boolean existsById(Long id) {
    return songJpa.existsById(id);
  }

  @Override
  @Transactional
  public int incrementPlayCount(Long id) {
    return songJpa.incrementPlayCount(id);
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    songJpa.deleteById(id);
  }
}
