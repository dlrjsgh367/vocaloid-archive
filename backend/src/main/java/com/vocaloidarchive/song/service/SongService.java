package com.vocaloidarchive.song.service;

import com.vocaloidarchive.character.domain.Character;
import com.vocaloidarchive.character.repository.CharacterRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.common.util.YoutubeUtil;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.tag.service.TagService;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

  private final SongRepository songRepository;
  private final CharacterRepository characterRepository;
  private final UserRepository userRepository;
  private final TagService tagService;
  private final SecurityUtil securityUtil;

  @Transactional
  public SongResponse create(SongCreateRequest req) {
    Long currentUserId = securityUtil.getCurrentUserId();
    User registeredBy = userRepository.getReferenceById(currentUserId);

    List<Character> characters = characterRepository.findAllById(req.characterIds());
    if (characters.size() != req.characterIds().size()) {
      throw new BusinessException(ErrorCode.CHARACTER_NOT_FOUND);
    }

    List<Tag> tags = tagService.findOrCreateAll(req.tagNames());
    String thumbnailUrl = YoutubeUtil.extractThumbnailUrl(req.youtubeUrl());

    Song song = Song.of(
        registeredBy, req.title(), req.youtubeUrl(), req.niconicoUrl(),
        thumbnailUrl, req.bpm(), req.mood());
    characters.forEach(song::addCharacter);
    tags.forEach(song::addTag);

    Song saved = songRepository.save(song);
    return SongResponse.from(saved, 0L);
  }
}
