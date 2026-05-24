package com.vocaloidarchive.share.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.playlist.application.GetPlaylistCardDataUseCase;
import com.vocaloidarchive.playlist.application.dto.result.PlaylistCardData;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OgSsrController.class)
@Import(SecurityConfig.class)
class OgSsrControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  @MockBean JwtAccessDeniedHandler jwtAccessDeniedHandler;
  @MockBean GetPlaylistCardDataUseCase getCardData;

  @Test
  @DisplayName("공개 플리 → OG 메타 포함 HTML 200")
  void publicPlaylist_ogHtml() throws Exception {
    given_card();

    mockMvc.perform(get("/share/p/CODE000001"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content().string(Matchers.containsString("property=\"og:image\"")))
        .andExpect(content().string(Matchers.containsString("/api/share/playlists/CODE000001/card.png")))
        .andExpect(content().string(Matchers.containsString("summary_large_image")))
        .andExpect(content().string(Matchers.containsString("My List")));
  }

  @Test
  @DisplayName("비공개/없음 → 404 HTML")
  void notFound_html() throws Exception {
    org.mockito.BDDMockito.given(getCardData.invoke("NOPENOPE12"))
        .willThrow(new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    mockMvc.perform(get("/share/p/NOPENOPE12"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("text/html"));
  }

  private void given_card() {
    PlaylistCardData card = new PlaylistCardData(10L, "CODE000001", "My List", "alice", 12, 3L,
        "#39C5BB", "미쿠", true,
        List.of(new PlaylistCardData.SongLine(1L, "S1", "t1", "BRIGHT")));
    org.mockito.BDDMockito.given(getCardData.invoke("CODE000001")).willReturn(card);
  }
}
