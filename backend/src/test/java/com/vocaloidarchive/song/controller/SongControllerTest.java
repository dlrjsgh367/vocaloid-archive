package com.vocaloidarchive.song.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.character.dto.response.CharacterResponse;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.song.dto.request.SongCreateRequest;
import com.vocaloidarchive.song.dto.response.SongDetailResponse;
import com.vocaloidarchive.song.dto.response.SongResponse;
import com.vocaloidarchive.song.dto.response.UserSummary;
import com.vocaloidarchive.song.service.SongService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class SongControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean SongService songService;

  private static final UserSummary USER_SUMMARY = new UserSummary(1L, "testuser");
  private static final List<CharacterResponse> CHARACTERS = List.of(
      new CharacterResponse(1L, "Hatsune Miku", "#39C5BB", null)
  );
  private static final List<String> TAGS = List.of("pop");
  private static final LocalDateTime NOW = LocalDateTime.now();

  private SongResponse sampleSongResponse() {
    return new SongResponse(1L, "Test Song", "https://img.youtube.com/vi/abc/0.jpg",
        Mood.BRIGHT, 0, 0L, USER_SUMMARY, CHARACTERS, TAGS, NOW);
  }

  private SongDetailResponse sampleSongDetailResponse() {
    return new SongDetailResponse(1L, "Test Song",
        "https://www.youtube.com/watch?v=abc", null,
        "https://img.youtube.com/vi/abc/0.jpg", 120, Mood.BRIGHT,
        0, 0L, USER_SUMMARY, CHARACTERS, TAGS, NOW);
  }

  @Test
  void search_anonymous_ok() throws Exception {
    PageResponse<SongResponse> page = PageResponse.from(
        new PageImpl<>(List.of(sampleSongResponse())));
    given(songService.search(any(), any(Pageable.class))).willReturn(page);

    mockMvc.perform(get("/api/songs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @Test
  void create_anonymous_unauthorized() throws Exception {
    SongCreateRequest req = new SongCreateRequest(
        "Test Song", "https://www.youtube.com/watch?v=abc", null,
        120, Mood.BRIGHT, List.of(1L), List.of("pop"));

    mockMvc.perform(post("/api/songs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void create_authenticated_ok() throws Exception {
    SongCreateRequest req = new SongCreateRequest(
        "Test Song", "https://www.youtube.com/watch?v=abc", null,
        120, Mood.BRIGHT, List.of(1L), List.of("pop"));
    given(songService.create(any())).willReturn(sampleSongResponse());

    mockMvc.perform(post("/api/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("Test Song"));
  }

  @Test
  @WithMockUser
  void create_validation_fail() throws Exception {
    SongCreateRequest req = new SongCreateRequest(
        "", "https://www.youtube.com/watch?v=abc", null,
        120, Mood.BRIGHT, List.of(1L), List.of("pop"));

    mockMvc.perform(post("/api/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.error.details").isMap());
  }

  @Test
  @WithMockUser
  void create_youtube_pattern_fail() throws Exception {
    SongCreateRequest req = new SongCreateRequest(
        "Test Song", "https://not-youtube.com/video", null,
        120, Mood.BRIGHT, List.of(1L), List.of("pop"));

    mockMvc.perform(post("/api/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void detail_anonymous_ok() throws Exception {
    given(songService.findDetail(1L)).willReturn(sampleSongDetailResponse());

    mockMvc.perform(get("/api/songs/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("Test Song"));
  }

  @Test
  void detail_not_found() throws Exception {
    given(songService.findDetail(99L))
        .willThrow(new BusinessException(ErrorCode.SONG_NOT_FOUND));

    mockMvc.perform(get("/api/songs/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("SONG_NOT_FOUND"));
  }

  @Test
  void delete_anonymous_unauthorized() throws Exception {
    mockMvc.perform(delete("/api/songs/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void delete_owner_ok() throws Exception {
    willDoNothing().given(songService).delete(1L);

    mockMvc.perform(delete("/api/songs/1")
            .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void delete_forbidden() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(songService).delete(2L);

    mockMvc.perform(delete("/api/songs/2")
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }
}
