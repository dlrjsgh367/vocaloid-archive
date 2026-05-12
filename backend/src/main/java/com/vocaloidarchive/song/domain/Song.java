package com.vocaloidarchive.song.domain;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "songs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Song {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registered_by", nullable = false)
  private UserEntity registeredBy;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "youtube_url", length = 500)
  private String youtubeUrl;

  @Column(name = "niconico_url", length = 500)
  private String niconicoUrl;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  private Integer bpm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "VARCHAR(20)")
  private Mood mood;

  @Column(name = "play_count", nullable = false)
  private Integer playCount = 0;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @BatchSize(size = 20)
  @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SongCharacter> characters = new ArrayList<>();

  @BatchSize(size = 20)
  @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SongTag> tags = new ArrayList<>();

  public static Song of(
      UserEntity registeredBy, String title, String youtubeUrl, String niconicoUrl,
      String thumbnailUrl, Integer bpm, Mood mood) {
    Song s = new Song();
    s.registeredBy = registeredBy;
    s.title = title;
    s.youtubeUrl = youtubeUrl;
    s.niconicoUrl = niconicoUrl;
    s.thumbnailUrl = thumbnailUrl;
    s.bpm = bpm;
    s.mood = mood;
    s.playCount = 0;
    return s;
  }

  public void addCharacter(CharacterEntity character) {
    characters.add(SongCharacter.of(this, character));
  }

  public void addTag(Tag tag) {
    tags.add(SongTag.of(this, tag));
  }
}
