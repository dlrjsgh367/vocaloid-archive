package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.song.infra.persistence.SongEntity;
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
public class LikeEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "song_id", nullable = false)
  private SongEntity song;

  @Column(name = "liked_at", nullable = false, insertable = false, updatable = false)
  private LocalDateTime likedAt;

  public static LikeEntity of(UserEntity user, SongEntity song) {
    LikeEntity like = new LikeEntity();
    like.user = user;
    like.song = song;
    return like;
  }
}
