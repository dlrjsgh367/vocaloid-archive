package com.vocaloidarchive.song.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.request.SongSearchRequest;
import com.vocaloidarchive.song.dto.response.SongDetailResponse;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

  private static final int MAX_PAGE_SIZE = 50;

  private final SongService songService;

  @GetMapping
  public ApiResponse<PageResponse<SongResponse>> search(
      @ModelAttribute SongSearchRequest request,
      @PageableDefault(size = 20) Pageable pageable) {
    Pageable capped = capPageSize(pageable);
    return ApiResponse.success(songService.search(request, capped));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SongResponse>> create(
      @RequestBody @Valid SongCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(songService.create(request)));
  }

  @GetMapping("/{id}")
  public ApiResponse<SongDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.success(songService.findDetail(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    songService.delete(id);
    return ResponseEntity.noContent().build();
  }

  private Pageable capPageSize(Pageable in) {
    int size = Math.min(Math.max(in.getPageSize(), 1), MAX_PAGE_SIZE);
    if (size == in.getPageSize()) return in;
    return org.springframework.data.domain.PageRequest.of(in.getPageNumber(), size, in.getSort());
  }
}
