package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.user.infra.persistence.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "playlists")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public static PlaylistEntity of(UserEntity user, String title, boolean isPublic) {
    PlaylistEntity p = new PlaylistEntity();
    p.user = user;
    p.title = title;
    p.isPublic = isPublic;
    return p;
  }
}
