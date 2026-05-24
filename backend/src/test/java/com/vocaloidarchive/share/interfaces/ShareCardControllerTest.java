package com.vocaloidarchive.share.interfaces;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.playlist.application.EnsureShareCodeUseCase;
import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import com.vocaloidarchive.share.application.RenderPlaylistCardUseCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShareCardController.class)
@Import(SecurityConfig.class)
class ShareCardControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  @MockBean JwtAccessDeniedHandler jwtAccessDeniedHandler;

  @MockBean EnsureShareCodeUseCase ensureShareCode;
  @MockBean GetPlaylistCardDataUseCase getCardData;
  @MockBean RenderPlaylistCardUseCase renderCard;

  private PlaylistCardData card() {
    return new PlaylistCardData(10L, "CODE000001", "My List", "alice", 1, 3L,
        "#39C5BB", "미쿠", true,
        List.of(new PlaylistCardData.SongLine(1L, "S1", "t1", "BRIGHT")));
  }

  @Test
  @WithMockUser
  @DisplayName("POST share: 인증 사용자 → shareCode + shareUrl")
  void share_authenticated_ok() throws Exception {
    given(ensureShareCode.invoke(10L)).willReturn("ABCDEF1234");

    mockMvc.perform(post("/api/playlists/10/share"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.shareCode").value("ABCDEF1234"))
        .andExpect(jsonPath("$.data.shareUrl").value(org.hamcrest.Matchers.containsString("/p/ABCDEF1234")));
  }

  @Test
  @DisplayName("GET 카드 데이터: 공개 → 200")
  void cardData_public_ok() throws Exception {
    given(getCardData.invoke("CODE000001")).willReturn(card());

    mockMvc.perform(get("/api/share/playlists/CODE000001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("My List"))
        .andExpect(jsonPath("$.data.themeColorHex").value("#39C5BB"))
        .andExpect(jsonPath("$.data.songs[0].mood").value("BRIGHT"));
  }

  @Test
  @DisplayName("GET 카드 데이터: 비공개/없음 → 404")
  void cardData_notFound() throws Exception {
    given(getCardData.invoke("NOPENOPE12"))
        .willThrow(new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    mockMvc.perform(get("/api/share/playlists/NOPENOPE12"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("PLAYLIST_NOT_FOUND"));
  }

  @Test
  @DisplayName("GET card.png: 200 image/png")
  void cardPng_ok() throws Exception {
    given(renderCard.invoke("CODE000001")).willReturn(new byte[] {(byte) 0x89, 'P', 'N', 'G'});

    mockMvc.perform(get("/api/share/playlists/CODE000001/card.png"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(org.springframework.http.MediaType.IMAGE_PNG))
        .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=300")));
  }

  @Test
  @DisplayName("GET card.png: 없음 → 404")
  void cardPng_notFound() throws Exception {
    given(renderCard.invoke(anyString()))
        .willThrow(new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    mockMvc.perform(get("/api/share/playlists/NOPENOPE12/card.png"))
        .andExpect(status().isNotFound());
  }
}
