package com.vocaloidarchive.playlist.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.playlist.domain.PlaylistSong;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.repository.PlaylistRepository;
import com.vocaloidarchive.playlist.repository.PlaylistSongRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

  @Mock PlaylistRepository playlistRepository;
  @Mock PlaylistSongRepository playlistSongRepository;
  @Mock SongRepository songRepository;
  @Mock UserJpaRepository userRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks PlaylistService playlistService;

  private UserEntity owner() {
    UserEntity u = UserEntity.of("alice", "alice@a.com", "hash");
    ReflectionTestUtils.setField(u, "id", 1L);
    return u;
  }

  private Playlist playlist(UserEntity u, boolean isPublic) {
    Playlist p = Playlist.of(u, "My List", isPublic);
    ReflectionTestUtils.setField(p, "id", 10L);
    return p;
  }

  // ── getMyPlaylists ────────────────────────────────────────────────────────

  @Test
  @DisplayName("getMyPlaylists: 본인 플레이리스트 목록 반환")
  void getMyPlaylists_returnsList() {
    UserEntity u = owner();
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findAllByUserId(1L)).willReturn(List.of(
        playlist(u, true), playlist(u, false)));
    given(playlistSongRepository.countByPlaylistId(10L)).willReturn(3L);

    List<PlaylistResponse> result = playlistService.getMyPlaylists();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).songCount()).isEqualTo(3);
  }

  // ── getDetail ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("getDetail: public playlist → 누구나 접근")
  void getDetail_public_ok() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(playlistSongRepository.findWithSongByPlaylistId(10L)).willReturn(List.of());

    PlaylistDetailResponse result = playlistService.getDetail(10L);

    assertThat(result.title()).isEqualTo("My List");
    assertThat(result.isPublic()).isTrue();
  }

  @Test
  @DisplayName("getDetail: private playlist → 본인 접근")
  void getDetail_private_ownerAccess() {
    UserEntity u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistSongRepository.findWithSongByPlaylistId(10L)).willReturn(List.of());

    PlaylistDetailResponse result = playlistService.getDetail(10L);

    assertThat(result.isPublic()).isFalse();
  }

  @Test
  @DisplayName("getDetail: private playlist → 타인 → FORBIDDEN")
  void getDetail_private_otherUser_forbidden() {
    UserEntity u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willReturn(2L);

    assertThatThrownBy(() -> playlistService.getDetail(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("getDetail: private playlist → 미인증 → FORBIDDEN")
  void getDetail_private_anonymous_forbidden() {
    UserEntity u = owner();
    Playlist p = playlist(u, false);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(securityUtil.getCurrentUserId()).willThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

    assertThatThrownBy(() -> playlistService.getDetail(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("getDetail: 없는 id → PLAYLIST_NOT_FOUND")
  void getDetail_notFound() {
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.getDetail(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create: 저장 후 PlaylistResponse 반환")
  void create_ok() {
    UserEntity u = owner();
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(userRepository.getReferenceById(1L)).willReturn(u);
    given(playlistRepository.save(any(Playlist.class))).willAnswer(inv -> inv.getArgument(0));

    PlaylistResponse result = playlistService.create(new PlaylistCreateRequest("New List", true));

    assertThat(result.title()).isEqualTo("New List");
    assertThat(result.isPublic()).isTrue();
    then(playlistRepository).should().save(any(Playlist.class));
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete: 본인 → repo.delete 호출")
  void delete_owner_ok() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    playlistService.delete(10L);

    then(playlistRepository).should().delete(p);
  }

  @Test
  @DisplayName("delete: 타인 → FORBIDDEN")
  void delete_forbidden() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.delete(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("delete: 없는 id → PLAYLIST_NOT_FOUND")
  void delete_notFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.delete(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  // ── addSong ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("addSong: 새 곡 추가, orderIndex = max+1")
  void addSong_newSong_savedWithOrder() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    Song song = Song.of(u, "T", null, null, null, null, Mood.BRIGHT);
    ReflectionTestUtils.setField(song, "id", 5L);

    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(5L)).willReturn(true);
    given(playlistSongRepository.existsByPlaylistIdAndSongId(10L, 5L)).willReturn(false);
    given(playlistSongRepository.findMaxOrderIndexByPlaylistId(10L)).willReturn(2);
    given(songRepository.getReferenceById(5L)).willReturn(song);

    playlistService.addSong(10L, new PlaylistSongAddRequest(5L));

    then(playlistSongRepository).should().save(any(PlaylistSong.class));
  }

  @Test
  @DisplayName("addSong: 이미 있는 곡 → skip (save 미호출)")
  void addSong_duplicate_skipped() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(5L)).willReturn(true);
    given(playlistSongRepository.existsByPlaylistIdAndSongId(10L, 5L)).willReturn(true);

    playlistService.addSong(10L, new PlaylistSongAddRequest(5L));

    then(playlistSongRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("addSong: playlist 없음 → PLAYLIST_NOT_FOUND")
  void addSong_playlistNotFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.addSong(99L, new PlaylistSongAddRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("addSong: 타인 → FORBIDDEN")
  void addSong_forbidden() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.addSong(10L, new PlaylistSongAddRequest(5L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("addSong: 곡 없음 → SONG_NOT_FOUND")
  void addSong_songNotFound() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));
    given(songRepository.existsById(99L)).willReturn(false);

    assertThatThrownBy(() -> playlistService.addSong(10L, new PlaylistSongAddRequest(99L)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  // ── removeSong ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("removeSong: 본인 → deleteByPlaylistIdAndSongId 호출")
  void removeSong_owner_ok() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    playlistService.removeSong(10L, 5L);

    then(playlistSongRepository).should().deleteByPlaylistIdAndSongId(10L, 5L);
  }

  @Test
  @DisplayName("removeSong: 타인 → FORBIDDEN")
  void removeSong_forbidden() {
    UserEntity u = owner();
    Playlist p = playlist(u, true);
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepository.findById(10L)).willReturn(Optional.of(p));

    assertThatThrownBy(() -> playlistService.removeSong(10L, 5L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("removeSong: playlist 없음 → PLAYLIST_NOT_FOUND")
  void removeSong_notFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepository.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> playlistService.removeSong(99L, 5L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }
}
