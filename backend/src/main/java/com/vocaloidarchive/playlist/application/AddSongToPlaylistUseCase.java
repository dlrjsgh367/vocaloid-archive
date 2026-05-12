package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.playlist.application.dto.command.AddSongToPlaylistCommand;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.application.port.PlaylistSongRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.song.infra.persistence.SongJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddSongToPlaylistUseCase {
  private final PlaylistRepository playlistRepo;
  private final PlaylistSongRepository songRepo;
  private final SongJpaRepository songJpa;

  @Transactional
  public void invoke(AddSongToPlaylistCommand cmd) {
    Playlist playlist = playlistRepo.findById(cmd.playlistId())
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
    if (!playlist.isOwnedBy(cmd.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (!songJpa.existsById(cmd.songId())) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    if (!songRepo.existsBy(cmd.playlistId(), cmd.songId())) {
      int next = songRepo.findMaxOrderIndex(cmd.playlistId()) + 1;
      songRepo.add(cmd.playlistId(), cmd.songId(), next);
    }
  }
}
