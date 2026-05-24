package com.vocaloidarchive.playlist.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.SecurityUtil;
import com.vocaloidarchive.playlist.application.port.PlaylistRepository;
import com.vocaloidarchive.playlist.domain.Playlist;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnsureShareCodeUseCaseTest {

  @Mock PlaylistRepository playlistRepo;
  @Mock SecurityUtil securityUtil;
  @InjectMocks EnsureShareCodeUseCase useCase;

  private Playlist playlist(boolean isPublic, String shareCode) {
    return Playlist.reconstitute(10L, 1L, "My List", isPublic, shareCode, LocalDateTime.now());
  }

  @Test
  @DisplayName("신규 발급: 코드 생성 후 저장하고 반환")
  void issuesNewCode() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(true, null)));
    given(playlistRepo.existsByShareCode(anyString())).willReturn(false);

    String code = useCase.invoke(10L);

    assertThat(code).matches("^[A-Za-z0-9]{10}$");
    verify(playlistRepo).updateShareCode(eq(10L), eq(code));
  }

  @Test
  @DisplayName("기존 코드: 재발급 없이 그대로 반환")
  void returnsExistingCode() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(true, "EXISTING01")));

    String code = useCase.invoke(10L);

    assertThat(code).isEqualTo("EXISTING01");
    verify(playlistRepo, never()).updateShareCode(anyLong(), anyString());
    verify(playlistRepo, never()).existsByShareCode(anyString());
  }

  @Test
  @DisplayName("비소유자 → FORBIDDEN")
  void nonOwnerForbidden() {
    given(securityUtil.getCurrentUserId()).willReturn(2L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(true, null)));

    assertThatThrownBy(() -> useCase.invoke(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("비공개 플리 → PLAYLIST_NOT_PUBLIC")
  void privatePlaylistRejected() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(false, null)));

    assertThatThrownBy(() -> useCase.invoke(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_PUBLIC);
  }

  @Test
  @DisplayName("없는 플리 → PLAYLIST_NOT_FOUND")
  void notFound() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.invoke(99L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("충돌 2회 후 성공")
  void retriesOnCollision() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(true, null)));
    given(playlistRepo.existsByShareCode(anyString())).willReturn(true, true, false);

    String code = useCase.invoke(10L);

    assertThat(code).matches("^[A-Za-z0-9]{10}$");
    verify(playlistRepo, times(3)).existsByShareCode(anyString());
    verify(playlistRepo, times(1)).updateShareCode(eq(10L), eq(code));
  }

  @Test
  @DisplayName("5회 모두 충돌 → SHARE_CODE_GENERATION_FAILED")
  void allCollisionsFail() {
    given(securityUtil.getCurrentUserId()).willReturn(1L);
    given(playlistRepo.findById(10L)).willReturn(Optional.of(playlist(true, null)));
    given(playlistRepo.existsByShareCode(anyString())).willReturn(true);

    assertThatThrownBy(() -> useCase.invoke(10L))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.SHARE_CODE_GENERATION_FAILED);
    verify(playlistRepo, never()).updateShareCode(eq(10L), anyString());
  }
}
