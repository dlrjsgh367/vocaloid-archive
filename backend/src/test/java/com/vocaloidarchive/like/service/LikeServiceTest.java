package com.vocaloidarchive.like.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.like.domain.LikeId;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.repository.LikeRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

  @Mock LikeRepository likeRepository;
  @Mock SongRepository songRepository;
  @Mock UserJpaRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks LikeService likeService;

  @Test
  void givenNotLiked_whenToggle_thenCreatesLikeAndReturnsLikedTrue() {
    // given
    Long userId = 1L, songId = 10L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(likeRepository.existsById(new LikeId(userId, songId))).willReturn(false);
    given(userRepository.getReferenceById(userId)).willReturn(UserEntity.of("u", "u@a.com", "h"));
    given(songRepository.getReferenceById(songId)).willReturn(
        Song.of(UserEntity.of("u", "u@a.com", "h"), "Title", null, null, null, null,
            com.vocaloidarchive.song.domain.Mood.BRIGHT));
    given(likeRepository.save(any(Like.class))).willAnswer(inv -> inv.getArgument(0));
    given(likeRepository.countBySongId(songId)).willReturn(5L);

    // when
    LikeToggleResponse res = likeService.toggle(songId);

    // then
    assertThat(res.liked()).isTrue();
    assertThat(res.likeCount()).isEqualTo(5L);
    then(likeRepository).should().save(any(Like.class));
  }

  @Test
  void givenAlreadyLiked_whenToggle_thenDeletesLikeAndReturnsLikedFalse() {
    // given
    Long userId = 1L, songId = 10L;
    LikeId likeId = new LikeId(userId, songId);
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(likeRepository.existsById(likeId)).willReturn(true);
    given(likeRepository.countBySongId(songId)).willReturn(3L);

    // when
    LikeToggleResponse res = likeService.toggle(songId);

    // then
    assertThat(res.liked()).isFalse();
    assertThat(res.likeCount()).isEqualTo(3L);
    then(likeRepository).should().deleteById(likeId);
  }

  @Test
  void givenSongNotFound_whenToggle_thenThrowsSongNotFound() {
    // given
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(songRepository.existsById(999L)).willReturn(false);

    // when / then
    assertThatThrownBy(() -> likeService.toggle(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }
}
