package com.vocaloidarchive.song.service;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.common.util.YoutubeUtil;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.request.SongSearchRequest;
import com.vocaloidarchive.song.dto.response.SongDetailResponse;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.repository.SongQueryRepository;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.tag.infra.persistence.TagEntity;
import com.vocaloidarchive.tag.service.TagService;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

  private final SongRepository songRepository;
  private final SongQueryRepository songQueryRepository;
  private final CharacterJpaRepository characterRepository;
  private final UserJpaRepository userRepository;
  private final TagService tagService;
  private final SecurityUtil securityUtil;

  @Transactional
  public SongResponse create(SongCreateRequest req) {
    Long currentUserId = securityUtil.getCurrentUserId();
    UserEntity registeredBy = userRepository.getReferenceById(currentUserId);

    List<CharacterEntity> characters = characterRepository.findAllById(req.characterIds());
    if (characters.size() != req.characterIds().size()) {
      throw new BusinessException(ErrorCode.CHARACTER_NOT_FOUND);
    }

    List<TagEntity> tags = tagService.findOrCreateAll(req.tagNames());
    String thumbnailUrl = YoutubeUtil.extractThumbnailUrl(req.youtubeUrl());

    Song song = Song.of(
        registeredBy, req.title(), req.youtubeUrl(), req.niconicoUrl(),
        thumbnailUrl, req.bpm(), req.mood());
    characters.forEach(song::addCharacter);
    tags.forEach(song::addTag);

    Song saved = songRepository.save(song);
    return SongResponse.from(saved, 0L);
  }

  public PageResponse<SongResponse> search(SongSearchRequest req, Pageable pageable) {
    Page<Song> page = songQueryRepository.search(req, pageable);
    Map<Long, Long> likeCounts = songQueryRepository.likeCountsFor(
        page.getContent().stream().map(Song::getId).toList());
    List<SongResponse> content = page.getContent().stream()
        .map(s -> SongResponse.from(s, likeCounts.getOrDefault(s.getId(), 0L)))
        .toList();
    return PageResponse.from(new PageImpl<>(content, pageable, page.getTotalElements()));
  }

  @Transactional
  public SongDetailResponse findDetail(Long id) {
    int affected = songRepository.incrementPlayCount(id);
    if (affected == 0) {
      throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
    }
    Song song = songRepository.findDetailWithCharacters(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.SONG_NOT_FOUND));
    songRepository.findDetailWithTags(id);
    long likeCount = songQueryRepository.likeCountFor(id);
    return SongDetailResponse.from(song, likeCount);
  }

  @Transactional
  public void delete(Long id) {
    Long currentUserId = securityUtil.getCurrentUserId();
    Song song = songRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.SONG_NOT_FOUND));
    if (!song.getRegisteredBy().getId().equals(currentUserId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    songRepository.delete(song);
  }
}
