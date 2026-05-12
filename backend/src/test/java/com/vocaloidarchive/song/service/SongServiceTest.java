package com.vocaloidarchive.song.service;

import com.vocaloidarchive.character.infra.persistence.CharacterEntity;
import com.vocaloidarchive.character.infra.persistence.CharacterJpaRepository;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.response.SongDetailResponse;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.repository.SongQueryRepository;
import com.vocaloidarchive.song.repository.SongRepository;
import com.vocaloidarchive.tag.application.FindOrCreateTagsUseCase;
import com.vocaloidarchive.tag.application.dto.result.TagResult;
import com.vocaloidarchive.tag.infra.persistence.TagEntity;
import com.vocaloidarchive.tag.infra.persistence.TagJpaRepository;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

  @Mock SongRepository songRepository;
  @Mock SongQueryRepository songQueryRepository;
  @Mock CharacterJpaRepository characterRepository;
  @Mock UserJpaRepository userRepository;
  @Mock FindOrCreateTagsUseCase findOrCreateTagsUseCase;
  @Mock TagJpaRepository tagJpaRepository;
  @Mock SecurityUtil securityUtil;
  @InjectMocks SongService songService;

  @Test
  void givenValidRequest_whenCreate_thenSavesSongWithCharactersAndTags() {
    // given
    Long userId = 1L;
    UserEntity user = UserEntity.of("user", "user@example.com", "hashed");
    CharacterEntity character = CharacterEntity.of("Miku", "#39C5BB", null);
    TagEntity tag = TagEntity.of("pop");

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
    TagResult tagResult = new TagResult(1L, "pop");
    given(findOrCreateTagsUseCase.invoke(List.of("pop"))).willReturn(List.of(tagResult));
    given(tagJpaRepository.getReferenceById(1L)).willReturn(tag);
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
    UserEntity user = UserEntity.of("user", "user@example.com", "hashed");

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
    UserEntity user = UserEntity.of("user", "user@example.com", "hashed");
    CharacterEntity character = CharacterEntity.of("Miku", "#39C5BB", null);

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
    given(findOrCreateTagsUseCase.invoke(null)).willReturn(List.of());
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
    UserEntity user = UserEntity.of("user", "user@example.com", "hashed");
    CharacterEntity character = CharacterEntity.of("Miku", "#39C5BB", null);

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
    given(findOrCreateTagsUseCase.invoke(null)).willReturn(List.of());
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
  @DisplayName("findDetail happy: incrementPlayCount → fetch join → SongDetailResponse")
  void findDetail_happy() {
    Long id = 5L;
    given(songRepository.incrementPlayCount(id)).willReturn(1);
    Song song = Song.of(
        UserEntity.of("a", "a@a.com", "h"), "T", null, null, null, null, Mood.BRIGHT);
    given(songRepository.findDetailWithCharacters(id)).willReturn(java.util.Optional.of(song));
    given(songRepository.findDetailWithTags(id)).willReturn(java.util.Optional.of(song));
    given(songQueryRepository.likeCountFor(id)).willReturn(3L);

    SongDetailResponse result = songService.findDetail(id);

    assertThat(result.title()).isEqualTo("T");
    assertThat(result.likeCount()).isEqualTo(3L);
    then(songRepository).should().incrementPlayCount(id);
  }

  @Test
  @DisplayName("findDetail: 없는 id → SONG_NOT_FOUND")
  void findDetail_missing() {
    given(songRepository.incrementPlayCount(99L)).willReturn(0);

    assertThatThrownBy(() -> songService.findDetail(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }

  @Test
  @DisplayName("delete: 본인 → repo.delete 호출")
  void delete_owner() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    UserEntity owner = UserEntity.of("a", "a@a.com", "h");
    org.springframework.test.util.ReflectionTestUtils.setField(owner, "id", 1L);
    Song song = Song.of(owner, "T", null, null, null, null, Mood.CALM);
    given(songRepository.findById(5L)).willReturn(java.util.Optional.of(song));

    songService.delete(5L);

    then(songRepository).should().delete(song);
  }

  @Test
  @DisplayName("delete: 타인 → FORBIDDEN")
  void delete_forbidden() {
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    UserEntity owner = UserEntity.of("a", "a@a.com", "h");
    org.springframework.test.util.ReflectionTestUtils.setField(owner, "id", 1L);
    Song song = Song.of(owner, "T", null, null, null, null, Mood.CALM);
    given(songRepository.findById(5L)).willReturn(java.util.Optional.of(song));

    assertThatThrownBy(() -> songService.delete(5L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("delete: 없는 id → SONG_NOT_FOUND")
  void delete_not_found() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(songRepository.findById(99L)).willReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> songService.delete(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SONG_NOT_FOUND);
  }
}
