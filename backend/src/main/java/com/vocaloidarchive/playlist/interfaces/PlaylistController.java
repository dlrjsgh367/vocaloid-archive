package com.vocaloidarchive.playlist.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.AddSongToPlaylistUseCase;
import com.vocaloidarchive.playlist.application.CreatePlaylistUseCase;
import com.vocaloidarchive.playlist.application.DeletePlaylistUseCase;
import com.vocaloidarchive.playlist.application.GetPlaylistDetailUseCase;
import com.vocaloidarchive.playlist.application.ListMyPlaylistsUseCase;
import com.vocaloidarchive.playlist.application.RemoveSongFromPlaylistUseCase;
import com.vocaloidarchive.playlist.application.dto.command.AddSongToPlaylistCommand;
import com.vocaloidarchive.playlist.application.dto.command.CreatePlaylistCommand;
import com.vocaloidarchive.playlist.interfaces.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.interfaces.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.interfaces.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.interfaces.dto.response.PlaylistResponse;
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

  private final ListMyPlaylistsUseCase listMyPlaylists;
  private final GetPlaylistDetailUseCase getPlaylistDetail;
  private final CreatePlaylistUseCase createPlaylist;
  private final DeletePlaylistUseCase deletePlaylist;
  private final AddSongToPlaylistUseCase addSongToPlaylist;
  private final RemoveSongFromPlaylistUseCase removeSongFromPlaylist;
  private final SecurityUtil securityUtil;

  @GetMapping
  public ResponseEntity<ApiResponse<List<PlaylistResponse>>> getMyPlaylists() {
    List<PlaylistResponse> result = listMyPlaylists.invoke().stream()
        .map(PlaylistResponse::from)
        .toList();
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<PlaylistResponse>> create(
      @RequestBody @Valid PlaylistCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    PlaylistResponse response = PlaylistResponse.from(
        createPlaylist.invoke(new CreatePlaylistCommand(userId, req.title(), req.isPublic())));
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PlaylistDetailResponse>> getDetail(@PathVariable Long id) {
    PlaylistDetailResponse response = PlaylistDetailResponse.from(getPlaylistDetail.invoke(id));
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    deletePlaylist.invoke(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/songs")
  public ResponseEntity<Void> addSong(
      @PathVariable Long id, @RequestBody @Valid PlaylistSongAddRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    addSongToPlaylist.invoke(new AddSongToPlaylistCommand(id, req.songId(), userId));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/songs/{songId}")
  public ResponseEntity<Void> removeSong(
      @PathVariable Long id, @PathVariable Long songId) {
    removeSongFromPlaylist.invoke(id, songId);
    return ResponseEntity.noContent().build();
  }
}
