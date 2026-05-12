package com.vocaloidarchive.song.application;

import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import com.vocaloidarchive.song.application.port.SongQueryRepository;
import com.vocaloidarchive.song.domain.Mood;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSongsUseCase {

  private final SongQueryRepository songQueryRepository;

  public PageResponse<SongResult> invoke(
      String keyword, Mood mood, Long characterId, Long tagId,
      SongSortKey sort, Pageable pageable) {
    SongSortKey effectiveSort = sort == null ? SongSortKey.LATEST : sort;
    return songQueryRepository.search(keyword, mood, characterId, tagId, effectiveSort, pageable);
  }
}
