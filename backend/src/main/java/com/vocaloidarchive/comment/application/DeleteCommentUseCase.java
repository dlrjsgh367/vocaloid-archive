package com.vocaloidarchive.comment.application;

import com.vocaloidarchive.comment.application.port.CommentRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCommentUseCase {
  private final CommentRepository commentRepository;
  private final SecurityUtil securityUtil;

  @Transactional
  public void invoke(Long commentId) {
    Long userId = securityUtil.getCurrentUserId();
    Long ownerId =
        commentRepository
            .findUserIdById(commentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    if (!ownerId.equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    commentRepository.deleteById(commentId);
  }
}
