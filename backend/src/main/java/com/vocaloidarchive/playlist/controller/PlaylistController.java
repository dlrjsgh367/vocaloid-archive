package com.vocaloidarchive.playlist.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistService playlistService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<PlaylistResponse>>> getMyPlaylists() {
    return ResponseEntity.ok(ApiResponse.success(playlistService.getMyPlaylists()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PlaylistResponse>> create(
      @RequestBody @Valid PlaylistCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(playlistService.create(req)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PlaylistDetailResponse>> getDetail(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(playlistService.getDetail(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    playlistService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/songs")
  public ResponseEntity<Void> addSong(
      @PathVariable Long id, @RequestBody @Valid PlaylistSongAddRequest req) {
    playlistService.addSong(id, req);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/songs/{songId}")
  public ResponseEntity<Void> removeSong(
      @PathVariable Long id, @PathVariable Long songId) {
    playlistService.removeSong(id, songId);
    return ResponseEntity.noContent().build();
  }
}
