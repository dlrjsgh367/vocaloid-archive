package com.vocaloidarchive.song.application.port;

import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.song.application.SongSortKey;
import com.vocaloidarchive.song.application.dto.result.SongDetailResult;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import com.vocaloidarchive.song.domain.Mood;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface SongQueryRepository {
  PageResponse<SongResult> search(
      String keyword, Mood mood, Long characterId, Long tagId,
      SongSortKey sort, Pageable pageable);
  Optional<SongDetailResult> findDetailById(Long id);
}
