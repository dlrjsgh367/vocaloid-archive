package com.vocaloidarchive.song.infra.persistence;

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
public class SongCharacterEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private SongEntity song;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "character_id", nullable = false)
  private CharacterEntity character;

  static SongCharacterEntity of(SongEntity song, CharacterEntity character) {
    SongCharacterEntity sc = new SongCharacterEntity();
    sc.song = song;
    sc.character = character;
    return sc;
  }
}
