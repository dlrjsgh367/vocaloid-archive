package com.vocaloidarchive.character.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterJpaRepository extends JpaRepository<CharacterEntity, Long> {
  List<CharacterEntity> findAllByOrderByIdAsc();
}
