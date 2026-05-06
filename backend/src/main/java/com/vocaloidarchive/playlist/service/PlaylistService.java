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
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final PlaylistSongRepository playlistSongRepository;
  private final SongRepository songRepository;
  private final UserRepository userRepository;
  private final SecurityUtil securityUtil;

  public List<PlaylistResponse> getMyPlaylists() {
    Long userId = securityUtil.getCurrentUserId();
    return playlistRepository.findAllByUserId(userId).stream()
        .map(p -> PlaylistResponse.from(p, playlistSongRepository.countByPlaylistId(p.getId())))
        .toList();
  }

  public PlaylistDetailResponse getDetail(Long id) {
    Playlist playlist = playlistRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isPublic()) {
      Long currentUserId;
      try {
        currentUserId = securityUtil.getCurrentUserId();
      } catch (BusinessException e) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      if (!playlist.getUser().getId().equals(currentUserId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
    }
    List<PlaylistSong> songs = playlistSongRepository.findWithSongByPlaylistId(id);
    return PlaylistDetailResponse.from(playlist, songs);
  }

  @Transactional
  public PlaylistResponse create(PlaylistCreateRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    User user = userRepository.getReferenceById(userId);
    Playlist saved = playlistRepository.save(Playlist.of(user, req.title(), req.isPublic()));
    return PlaylistResponse.from(saved, 0);
  }

  @Transactional
  public void delete(Long id) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    playlistRepository.delete(playlist);
  }

  @Transactional
  public void addSong(Long playlistId, PlaylistSongAddRequest req) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!songRepository.existsById(req.songId())) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    if (!playlistSongRepository.existsByPlaylistIdAndSongId(playlistId, req.songId())) {
      int nextOrder = playlistSongRepository.findMaxOrderIndexByPlaylistId(playlistId) + 1;
      Song song = songRepository.getReferenceById(req.songId());
      playlistSongRepository.save(PlaylistSong.of(playlist, song, nextOrder));
    }
  }

  @Transactional
  public void removeSong(Long playlistId, Long songId) {
    Long userId = securityUtil.getCurrentUserId();
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
  }
}
