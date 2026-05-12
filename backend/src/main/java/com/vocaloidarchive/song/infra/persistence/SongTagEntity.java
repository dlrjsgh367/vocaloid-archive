package com.vocaloidarchive.song.infra.persistence;

import com.vocaloidarchive.tag.infra.persistence.TagEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "song_tags")
@IdClass(SongTagId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SongTagEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private SongEntity song;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id", nullable = false)
  private TagEntity tag;

  static SongTagEntity of(SongEntity song, TagEntity tag) {
    SongTagEntity st = new SongTagEntity();
    st.song = song;
    st.tag = tag;
    return st;
  }
}
