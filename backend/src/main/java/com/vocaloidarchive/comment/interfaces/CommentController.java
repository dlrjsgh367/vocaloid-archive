package com.vocaloidarchive.comment.interfaces;

import com.vocaloidarchive.comment.application.CreateCommentUseCase;
import com.vocaloidarchive.comment.application.DeleteCommentUseCase;
import com.vocaloidarchive.comment.application.ListCommentsUseCase;
import com.vocaloidarchive.comment.application.dto.command.CreateCommentCommand;
import com.vocaloidarchive.comment.application.dto.result.CommentResult;
import com.vocaloidarchive.comment.interfaces.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.interfaces.dto.response.CommentResponse;
import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

  private static final int MAX_PAGE_SIZE = 50;

  private final ListCommentsUseCase listCommentsUseCase;
  private final CreateCommentUseCase createCommentUseCase;
  private final DeleteCommentUseCase deleteCommentUseCase;
  private final SecurityUtil securityUtil;

  @GetMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> list(
      @PathVariable Long id,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Pageable capped = capPageSize(pageable);
    PageResponse<CommentResult> page = listCommentsUseCase.invoke(id, capped);
    PageResponse<CommentResponse> mapped =
        new PageResponse<>(
            page.content().stream().map(CommentResponse::from).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages());
    return ResponseEntity.ok(ApiResponse.success(mapped));
  }

  @PostMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<CommentResponse>> create(
      @PathVariable Long id, @RequestBody @Valid CommentCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                CommentResponse.from(
                    createCommentUseCase.invoke(
                        new CreateCommentCommand(id, userId, req.content())))));
  }

  @DeleteMapping("/api/comments/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    deleteCommentUseCase.invoke(id);
    return ResponseEntity.noContent().build();
  }

  private Pageable capPageSize(Pageable in) {
    int size = Math.min(Math.max(in.getPageSize(), 1), MAX_PAGE_SIZE);
    if (size == in.getPageSize()) return in;
    return PageRequest.of(in.getPageNumber(), size, in.getSort());
  }
}
