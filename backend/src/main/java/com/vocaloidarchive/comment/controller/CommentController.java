package com.vocaloidarchive.comment.controller;

import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.service.CommentService;
import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.response.PageResponse;
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

  private final CommentService commentService;

  @GetMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> list(
      @PathVariable Long id,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Pageable capped = capPageSize(pageable);
    return ResponseEntity.ok(ApiResponse.success(commentService.list(id, capped)));
  }

  @PostMapping("/api/songs/{id}/comments")
  public ResponseEntity<ApiResponse<CommentResponse>> create(
      @PathVariable Long id,
      @RequestBody @Valid CommentCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(commentService.create(id, req)));
  }

  @DeleteMapping("/api/comments/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    commentService.delete(id);
    return ResponseEntity.noContent().build();
  }

  private Pageable capPageSize(Pageable in) {
    int size = Math.min(Math.max(in.getPageSize(), 1), MAX_PAGE_SIZE);
    if (size == in.getPageSize()) return in;
    return PageRequest.of(in.getPageNumber(), size, in.getSort());
  }
}
