package com.vocaloidarchive.comment.service;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.repository.CommentRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock CommentRepository commentRepository;
  @Mock SongRepository songRepository;
  @Mock UserJpaRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks CommentService commentService;

  private UserEntity makeUser(String name) {
    return UserEntity.of(name, name + "@a.com", "hash");
  }

  private Song makeSong(UserEntity owner) {
    return Song.of(owner, "Title", null, null, null, null, Mood.BRIGHT);
  }

  @Test
  void givenSongExists_whenList_thenReturnsPageResponse() {
    UserEntity user = makeUser("alice");
    Song song = makeSong(user);
    Comment comment = Comment.of(user, song, "hello");
    PageRequest pageable = PageRequest.of(0, 20);

    given(songRepository.existsById(10L)).willReturn(true);
    given(commentRepository.findWithUserBySongId(10L, pageable))
        .willReturn(new PageImpl<>(List.of(comment), pageable, 1));

    PageResponse<CommentResponse> result = commentService.list(10L, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).content()).isEqualTo("hello");
  }

  @Test
  void givenSongNotFound_whenList_thenThrows() {
    given(songRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.list(999L, PageRequest.of(0, 20)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  @Test
  void givenValidRequest_whenCreate_thenSavesComment() {
    Long userId = 1L, songId = 10L;
    UserEntity user = makeUser("alice");
    Song song = makeSong(user);
    Comment comment = Comment.of(user, song, "great song");
    CommentCreateRequest req = new CommentCreateRequest("great song");

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(songRepository.existsById(songId)).willReturn(true);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(songRepository.getReferenceById(songId)).willReturn(song);
    given(commentRepository.save(any(Comment.class))).willReturn(comment);

    CommentResponse res = commentService.create(songId, req);

    assertThat(res.content()).isEqualTo("great song");
    then(commentRepository).should().save(any(Comment.class));
  }

  @Test
  void givenSongNotFound_whenCreate_thenThrows() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(songRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.create(999L, new CommentCreateRequest("text")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  @Test
  void givenOwner_whenDelete_thenDeletesComment() {
    Long userId = 1L, commentId = 5L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(commentRepository.existsById(commentId)).willReturn(true);
    given(commentRepository.existsByIdAndUserId(commentId, userId)).willReturn(true);

    commentService.delete(commentId);

    then(commentRepository).should().deleteById(commentId);
  }

  @Test
  void givenNotOwner_whenDelete_thenThrowsForbidden() {
    Long userId = 2L, commentId = 5L;
    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(commentRepository.existsById(commentId)).willReturn(true);
    given(commentRepository.existsByIdAndUserId(commentId, userId)).willReturn(false);

    assertThatThrownBy(() -> commentService.delete(commentId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void givenCommentNotFound_whenDelete_thenThrows() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(commentRepository.existsById(999L)).willReturn(false);
    assertThatThrownBy(() -> commentService.delete(999L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
  }
}
