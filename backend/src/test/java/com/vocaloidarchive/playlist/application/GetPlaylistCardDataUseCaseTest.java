package com.vocaloidarchive.playlist.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.playlist.application.port.PlaylistQueryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetPlaylistCardDataUseCaseTest {

  @Mock PlaylistQueryRepository queryRepo;
  @InjectMocks GetPlaylistCardDataUseCase useCase;

  private PlaylistCardData card(boolean isPublic) {
    return new PlaylistCardData(10L, "CODE000001", "My List", "alice", 1, 3L,
        "#39C5BB", "미쿠", isPublic, List.of());
  }

  @Test
  @DisplayName("공개 플리 → 카드 데이터 반환")
  void publicReturned() {
    given(queryRepo.findCardDataByShareCode("CODE000001")).willReturn(Optional.of(card(true)));

    PlaylistCardData data = useCase.invoke("CODE000001");

    assertThat(data.title()).isEqualTo("My List");
    assertThat(data.themeColorHex()).isEqualTo("#39C5BB");
  }

  @Test
  @DisplayName("비공개 플리 → PLAYLIST_NOT_FOUND (존재 노출 회피)")
  void privateReportedAsNotFound() {
    given(queryRepo.findCardDataByShareCode("CODE000001")).willReturn(Optional.of(card(false)));

    assertThatThrownBy(() -> useCase.invoke("CODE000001"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }

  @Test
  @DisplayName("없는 code → PLAYLIST_NOT_FOUND")
  void unknownCodeNotFound() {
    given(queryRepo.findCardDataByShareCode("NOPENOPE12")).willReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.invoke("NOPENOPE12"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND);
  }
}
