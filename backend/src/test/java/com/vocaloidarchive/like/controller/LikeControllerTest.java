package com.vocaloidarchive.like.controller;

import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.service.LikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikeController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class LikeControllerTest {

  @Autowired MockMvc mockMvc;
  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean LikeService likeService;

  @Test
  @WithMockUser
  void toggle_authenticated_returns200WithLikeData() throws Exception {
    given(likeService.toggle(1L)).willReturn(new LikeToggleResponse(true, 5L));

    mockMvc.perform(post("/api/songs/1/like").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.liked").value(true))
        .andExpect(jsonPath("$.data.likeCount").value(5));
  }

  @Test
  void toggle_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/songs/1/like").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void toggle_songNotFound_returns404() throws Exception {
    given(likeService.toggle(999L))
        .willThrow(new BusinessException(ErrorCode.SONG_NOT_FOUND));

    mockMvc.perform(post("/api/songs/999/like").with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("SONG_NOT_FOUND"));
  }
}
