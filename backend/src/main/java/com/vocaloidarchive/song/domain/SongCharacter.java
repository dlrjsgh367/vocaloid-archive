package com.vocaloidarchive.song.domain;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "song_characters")
@IdClass(SongCharacterId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongCharacter {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "character_id", nullable = false)
  private CharacterEntity character;

  static SongCharacter of(Song song, CharacterEntity character) {
    SongCharacter sc = new SongCharacter();
    sc.song = song;
    sc.character = character;
    return sc;
  }
}
