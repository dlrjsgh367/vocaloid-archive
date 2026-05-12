package com.vocaloidarchive.playlist.application;

import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistResult;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyPlaylistsUseCase {
  private final PlaylistQueryRepository queryRepo;
  private final SecurityUtil securityUtil;

  @Transactional(readOnly = true)
  public List<PlaylistResult> invoke() {
    return queryRepo.findMyPlaylists(securityUtil.getCurrentUserId());
  }
}
