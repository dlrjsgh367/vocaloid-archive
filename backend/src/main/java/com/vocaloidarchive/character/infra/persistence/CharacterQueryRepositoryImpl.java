package com.vocaloidarchive.character.infra.persistence;

import com.vocaloidarchive.character.application.dto.result.CharacterResult;
import com.vocaloidarchive.character.application.dto.result.CharacterSummaryResult;
import com.vocaloidarchive.character.application.port.CharacterQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CharacterQueryRepositoryImpl implements CharacterQueryRepository {

  private final CharacterJpaRepository jpa;
  private final EntityManager em;

  @Override
  public List<CharacterResult> findAllOrderByIdAsc() {
    return jpa.findAllByOrderByIdAsc().stream()
        .map(e -> new CharacterResult(e.getId(), e.getName(), e.getColorHex(), e.getImageUrl()))
        .toList();
  }

  @Override
  public List<CharacterSummaryResult> findAllSummariesOrderByIdAsc() {
    return em.createQuery(
            "SELECT new com.vocaloidarchive.character.application.dto.result.CharacterSummaryResult("
                + "c.id, c.name, c.colorHex, c.imageUrl, COUNT(sc.song.id)) "
                + "FROM com.vocaloidarchive.character.infra.persistence.CharacterEntity c "
                + "LEFT JOIN com.vocaloidarchive.song.infra.persistence.SongCharacterEntity sc "
                + "ON sc.character = c "
                + "GROUP BY c.id, c.name, c.colorHex, c.imageUrl "
                + "ORDER BY c.id ASC",
            CharacterSummaryResult.class)
        .getResultList();
  }
}
