package com.vocaloidarchive.comment.application;

import com.vocaloidarchive.comment.application.dto.command.CreateCommentCommand;
import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import com.vocaloidarchive.comment.application.port.CommentRepository;
import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.song.application.port.SongRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommentUseCase {
  private final CommentRepository commentRepository;
  private final SongRepository songRepository;
  private final UserRepository userRepository;

  @Transactional
  public CommentResult invoke(CreateCommentCommand cmd) {
    if (!songRepository.existsById(cmd.songId())) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    Comment saved =
        commentRepository.save(
            Comment.newComment(
                User.reference(cmd.userId()), Song.reference(cmd.songId()), cmd.content()));
    User user =
        userRepository
            .findById(cmd.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    return new CommentResult(saved.getId(), saved.getContent(), user.getUsername(),
        saved.getCreatedAt());
  }
}
