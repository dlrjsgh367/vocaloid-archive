package com.vocaloidarchive.comment.application.port;

import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import com.vocaloidarchive.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CommentQueryRepository {
  PageResponse<CommentResult> listBySong(Long songId, Pageable pageable);
}
