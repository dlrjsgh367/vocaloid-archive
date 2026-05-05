package com.vocaloidarchive.character.repository;

import com.vocaloidarchive.character.domain.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {
  List<Character> findAllByOrderByIdAsc();
}
