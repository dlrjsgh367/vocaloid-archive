package com.vocaloidarchive.song.repository;

import com.vocaloidarchive.song.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Song s set s.playCount = s.playCount + 1 where s.id = :id")
  int incrementPlayCount(@Param("id") Long id);

  @Query("select distinct s from Song s "
      + "join fetch s.registeredBy "
      + "left join fetch s.characters sc "
      + "left join fetch sc.character "
      + "where s.id = :id")
  Optional<Song> findDetailWithCharacters(@Param("id") Long id);

  @Query("select distinct s from Song s "
      + "left join fetch s.tags st "
      + "left join fetch st.tag "
      + "where s.id = :id")
  Optional<Song> findDetailWithTags(@Param("id") Long id);
}
