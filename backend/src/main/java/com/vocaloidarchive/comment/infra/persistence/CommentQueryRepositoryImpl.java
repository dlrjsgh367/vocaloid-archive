package com.vocaloidarchive.comment.infra.persistence;

import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import com.vocaloidarchive.comment.application.port.CommentQueryRepository;
import com.vocaloidarchive.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {
  private final CommentJpaRepository jpa;

  @Override
  public PageResponse<CommentResult> listBySong(Long songId, Pageable pageable) {
    return PageResponse.from(
        jpa.findWithUserBySongId(songId, pageable)
            .map(
                e ->
                    new CommentResult(
                        e.getId(), e.getContent(), e.getUser().getUsername(), e.getCreatedAt())));
  }
}
