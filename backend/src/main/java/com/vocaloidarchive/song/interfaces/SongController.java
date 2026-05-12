package com.vocaloidarchive.song.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.application.CreateSongUseCase;
import com.vocaloidarchive.song.application.DeleteSongUseCase;
import com.vocaloidarchive.song.application.GetSongDetailUseCase;
import com.vocaloidarchive.song.application.SearchSongsUseCase;
import com.vocaloidarchive.song.application.dto.command.CreateSongCommand;
import com.vocaloidarchive.song.interfaces.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.interfaces.dto.request.SongSearchRequest;
import com.vocaloidarchive.song.interfaces.dto.response.SongDetailResponse;
import com.vocaloidarchive.song.interfaces.dto.response.SongResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

  private static final int MAX_PAGE_SIZE = 50;

  private final SearchSongsUseCase searchSongsUseCase;
  private final GetSongDetailUseCase getSongDetailUseCase;
  private final CreateSongUseCase createSongUseCase;
  private final DeleteSongUseCase deleteSongUseCase;
  private final SecurityUtil securityUtil;

  @GetMapping
  public ApiResponse<PageResponse<SongResponse>> search(
      @ModelAttribute SongSearchRequest request,
      @PageableDefault(size = 20) Pageable pageable) {
    Pageable capped = capPageSize(pageable);
    PageResponse<com.vocaloidarchive.song.application.dto.result.SongResult> result =
        searchSongsUseCase.invoke(
            request.keyword(), request.mood(), request.characterId(), request.tagId(),
            request.sortOrDefault().toSortKey(), capped);
    PageResponse<SongResponse> mapped = new PageResponse<>(
        result.content().stream().map(SongResponse::from).toList(),
        result.page(), result.size(), result.totalElements(), result.totalPages());
    return ApiResponse.success(mapped);
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SongResponse>> create(
      @RequestBody @Valid SongCreateRequest request) {
    Long userId = securityUtil.getCurrentUserId();
    CreateSongCommand cmd = new CreateSongCommand(
        userId, request.title(), request.youtubeUrl(), request.niconicoUrl(),
        request.bpm(), request.mood(), request.characterIds(), request.tagNames());
    SongResponse response = SongResponse.from(createSongUseCase.invoke(cmd));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
  }

  @GetMapping("/{id}")
  public ApiResponse<SongDetailResponse> detail(@PathVariable Long id) {
    return ApiResponse.success(SongDetailResponse.from(getSongDetailUseCase.invoke(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    deleteSongUseCase.invoke(id);
    return ResponseEntity.noContent().build();
  }

  private Pageable capPageSize(Pageable in) {
    int size = Math.min(Math.max(in.getPageSize(), 1), MAX_PAGE_SIZE);
    if (size == in.getPageSize()) return in;
    return PageRequest.of(in.getPageNumber(), size, in.getSort());
  }
}
