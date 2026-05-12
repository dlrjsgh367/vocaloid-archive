package com.vocaloidarchive.song.domain;

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
public class SongTag {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id", nullable = false)
  private TagEntity tag;

  static SongTag of(Song song, TagEntity tag) {
    SongTag st = new SongTag();
    st.song = song;
    st.tag = tag;
    return st;
  }
}
