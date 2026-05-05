package com.vocaloidarchive.song.service;

import com.vocaloidarchive.character.domain.Character;
import com.vocaloidarchive.character.repository.CharacterRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.tag.domain.Tag;
import com.vocaloidarchive.tag.service.TagService;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

  @Mock SongRepository songRepository;
  @Mock CharacterRepository characterRepository;
  @Mock UserRepository userRepository;
  @Mock TagService tagService;
  @Mock SecurityUtil securityUtil;
  @InjectMocks SongService songService;

  @Test
  void givenValidRequest_whenCreate_thenSavesSongWithCharactersAndTags() {
    // given
    Long userId = 1L;
    User user = User.of("user", "user@example.com", "hashed");
    Character character = Character.of("Miku", "#39C5BB", null);
    Tag tag = Tag.of("pop");

    SongCreateRequest req = new SongCreateRequest(
        "Test Song",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        null,
        120,
        Mood.BRIGHT,
        List.of(1L),
        List.of("pop"));

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(characterRepository.findAllById(List.of(1L))).willReturn(List.of(character));
    given(tagService.findOrCreateAll(List.of("pop"))).willReturn(List.of(tag));
    given(songRepository.save(any(Song.class))).willAnswer(inv -> inv.getArgument(0));

    // when
    SongResponse response = songService.create(req);

    // then
    ArgumentCaptor<Song> captor = ArgumentCaptor.forClass(Song.class);
    verify(songRepository).save(captor.capture());
    Song saved = captor.getValue();

    assertThat(saved.getTitle()).isEqualTo("Test Song");
    assertThat(saved.getThumbnailUrl()).isEqualTo(
        "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    assertThat(saved.getCharacters()).hasSize(1);
    assertThat(saved.getTags()).hasSize(1);

    assertThat(response.likeCount()).isEqualTo(0L);
    assertThat(response.thumbnailUrl()).isEqualTo(
        "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
  }

  @Test
  void givenMissingCharacter_whenCreate_thenThrowsCharacterNotFound() {
    // given
    Long userId = 1L;
    User user = User.of("user", "user@example.com", "hashed");

    SongCreateRequest req = new SongCreateRequest(
        "Test Song",
        null,
        null,
        120,
        Mood.BRIGHT,
        List.of(1L, 99L),
        null);

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(characterRepository.findAllById(List.of(1L, 99L))).willReturn(List.of());

    // when / then
    assertThatThrownBy(() -> songService.create(req))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.CHARACTER_NOT_FOUND));
  }

  @Test
  void givenNonYoutubeUrl_whenCreate_thenThumbnailUrlIsNull() {
    // given
    Long userId = 1L;
    User user = User.of("user", "user@example.com", "hashed");
    Character character = Character.of("Miku", "#39C5BB", null);

    SongCreateRequest req = new SongCreateRequest(
        "Test Song",
        "https://www.youtube.com/channel/UCfake",
        null,
        120,
        Mood.BRIGHT,
        List.of(1L),
        null);

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(characterRepository.findAllById(List.of(1L))).willReturn(List.of(character));
    given(tagService.findOrCreateAll(null)).willReturn(List.of());
    given(songRepository.save(any(Song.class))).willAnswer(inv -> inv.getArgument(0));

    // when
    SongResponse response = songService.create(req);

    // then
    ArgumentCaptor<Song> captor = ArgumentCaptor.forClass(Song.class);
    verify(songRepository).save(captor.capture());
    assertThat(captor.getValue().getThumbnailUrl()).isNull();
    assertThat(response.thumbnailUrl()).isNull();
  }

  @Test
  void givenNullYoutubeUrl_whenCreate_thenThumbnailUrlIsNull() {
    // given
    Long userId = 1L;
    User user = User.of("user", "user@example.com", "hashed");
    Character character = Character.of("Miku", "#39C5BB", null);

    SongCreateRequest req = new SongCreateRequest(
        "Test Song",
        null,
        null,
        120,
        Mood.BRIGHT,
        List.of(1L),
        null);

    given(securityUtil.getCurrentUserId()).willReturn(userId);
    given(userRepository.getReferenceById(userId)).willReturn(user);
    given(characterRepository.findAllById(List.of(1L))).willReturn(List.of(character));
    given(tagService.findOrCreateAll(null)).willReturn(List.of());
    given(songRepository.save(any(Song.class))).willAnswer(inv -> inv.getArgument(0));

    // when
    SongResponse response = songService.create(req);

    // then
    ArgumentCaptor<Song> captor = ArgumentCaptor.forClass(Song.class);
    verify(songRepository).save(captor.capture());
    assertThat(captor.getValue().getThumbnailUrl()).isNull();
    assertThat(response.thumbnailUrl()).isNull();
  }
}
