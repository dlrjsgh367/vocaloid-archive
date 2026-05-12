package com.vocaloidarchive.like.domain;

import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes")
@IdClass(LikeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private Song song;

  @Column(name = "liked_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime likedAt;

  public static Like of(UserEntity user, Song song) {
    Like like = new Like();
    like.user = user;
    like.song = song;
    return like;
  }
}
