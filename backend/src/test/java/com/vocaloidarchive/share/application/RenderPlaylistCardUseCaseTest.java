package com.vocaloidarchive.share.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.share.application.dto.CardRenderRequest;
import com.vocaloidarchive.share.application.port.CardCachePort;
import com.vocaloidarchive.share.application.port.CardRendererPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenderPlaylistCardUseCaseTest {

  @Mock GetPlaylistCardDataUseCase getCardData;
  @Mock CardRendererPort renderer;
  @Mock CardCachePort cache;
  @InjectMocks RenderPlaylistCardUseCase useCase;

  private PlaylistCardData card() {
    return new PlaylistCardData(10L, "CODE000001", "My List", "alice", 2, 5L,
        "#39C5BB", "미쿠", true,
        List.of(new PlaylistCardData.SongLine(1L, "S1", "t1", "BRIGHT"),
            new PlaylistCardData.SongLine(2L, "S2", "t2", "CALM")));
  }

  @Test
  @DisplayName("캐시 히트 → 렌더러 호출 안 함")
  void cacheHit_noRender() {
    given(getCardData.invoke("CODE000001")).willReturn(card());
    given(cache.get(eq("CODE000001"), anyString())).willReturn(Optional.of(new byte[] {1, 2, 3}));

    byte[] png = useCase.invoke("CODE000001");

    assertThat(png).containsExactly(1, 2, 3);
    verify(renderer, never()).render(any());
    verify(cache, never()).put(anyString(), anyString(), any());
  }

  @Test
  @DisplayName("캐시 미스 → 렌더 후 캐시에 저장")
  void cacheMiss_rendersAndCaches() {
    given(getCardData.invoke("CODE000001")).willReturn(card());
    given(cache.get(eq("CODE000001"), anyString())).willReturn(Optional.empty());
    given(renderer.render(any(CardRenderRequest.class))).willReturn(new byte[] {9, 9});

    byte[] png = useCase.invoke("CODE000001");

    assertThat(png).containsExactly(9, 9);
    verify(renderer).render(any(CardRenderRequest.class));
    verify(cache).put(eq("CODE000001"), anyString(), eq(new byte[] {9, 9}));
  }

  @Test
  @DisplayName("렌더러 실패 → CARD_RENDER_FAILED 전파")
  void rendererFails_propagates() {
    given(getCardData.invoke("CODE000001")).willReturn(card());
    given(cache.get(eq("CODE000001"), anyString())).willReturn(Optional.empty());
    given(renderer.render(any(CardRenderRequest.class)))
        .willThrow(new BusinessException(ErrorCode.CARD_RENDER_FAILED));

    assertThatThrownBy(() -> useCase.invoke("CODE000001"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.CARD_RENDER_FAILED);
    verify(cache, never()).put(anyString(), anyString(), any());
  }
}
