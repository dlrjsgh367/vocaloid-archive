package com.vocaloidarchive.comment.service;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.repository.CommentRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final SongRepository songRepository;
  private final UserJpaRepository userRepository;
  private final SecurityUtil securityUtil;

  public PageResponse<CommentResponse> list(Long songId, Pageable pageable) {
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    return PageResponse.from(
        commentRepository.findWithUserBySongId(songId, pageable)
            .map(CommentResponse::from));
  }

  @Transactional
  public CommentResponse create(Long songId, CommentCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    if (!songRepository.existsById(songId)) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    var user = userRepository.getReferenceById(userId);
    var song = songRepository.getReferenceById(songId);
    Comment saved = commentRepository.save(Comment.of(user, song, req.content()));
    return CommentResponse.from(saved);
  }

  @Transactional
  public void delete(Long commentId) {
    Long userId = securityUtil.getCurrentUserId();
    if (!commentRepository.existsById(commentId)) {
      throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
    }
    if (!commentRepository.existsByIdAndUserId(commentId, userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    commentRepository.deleteById(commentId);
  }
}
