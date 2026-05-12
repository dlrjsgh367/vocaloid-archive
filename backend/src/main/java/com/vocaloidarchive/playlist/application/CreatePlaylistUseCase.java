package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.playlist.application.dto.command.CreatePlaylistCommand;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePlaylistUseCase {
  private final PlaylistRepository playlistRepo;
  private final PlaylistQueryRepository queryRepo;

  @Transactional
  public PlaylistResult invoke(CreatePlaylistCommand cmd) {
    Playlist saved = playlistRepo.save(
        Playlist.newPlaylist(cmd.userId(), cmd.title(), cmd.isPublic()));
    return queryRepo.findResultAfterCreate(saved.getId());
  }
}
