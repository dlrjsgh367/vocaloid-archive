package com.vocaloidarchive.song.application;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.util.YoutubeUtil;
import com.vocaloidarchive.song.application.dto.command.CreateSongCommand;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import com.vocaloidarchive.song.application.port.SongRepository;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.tag.application.FindOrCreateTagsUseCase;
import com.vocaloidarchive.tag.application.dto.result.TagResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSongUseCase {

  private final SongRepository songRepository;
  private final CharacterJpaRepository characterJpaRepository;
  private final FindOrCreateTagsUseCase findOrCreateTagsUseCase;

  @Transactional
  public SongResult invoke(CreateSongCommand cmd) {
    List<CharacterEntity> characters = characterJpaRepository.findAllById(cmd.characterIds());
    if (characters.size() != cmd.characterIds().size()) {
      throw new BusinessException(ErrorCode.CHARACTER_NOT_FOUND);
    }

    List<TagResult> tagResults = findOrCreateTagsUseCase.invoke(cmd.tagNames());
    List<Long> tagIds = tagResults.stream().map(TagResult::id).toList();

    String thumbnailUrl = YoutubeUtil.extractThumbnailUrl(cmd.youtubeUrl());

    Song song = Song.newSong(
        cmd.userId(), cmd.title(), cmd.youtubeUrl(), cmd.niconicoUrl(),
        thumbnailUrl, cmd.bpm(), cmd.mood());

    Song saved = songRepository.save(song, cmd.characterIds(), tagIds);

    SongResult.Owner owner = new SongResult.Owner(saved.getRegisteredById(), null);
    List<SongResult.CharacterRef> characterRefs = characters.stream()
        .map(c -> new SongResult.CharacterRef(c.getId(), c.getName(), c.getColorHex(), c.getImageUrl()))
        .toList();
    List<String> tagNames = tagResults.stream().map(TagResult::name).toList();

    return new SongResult(saved.getId(), saved.getTitle(), saved.getThumbnailUrl(),
        saved.getMood(), saved.getPlayCount(), 0L, owner, characterRefs, tagNames, saved.getCreatedAt());
  }
}
