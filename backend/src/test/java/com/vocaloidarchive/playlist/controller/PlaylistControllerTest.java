package com.vocaloidarchive.playlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.playlist.dto.request.PlaylistCreateRequest;
import com.vocaloidarchive.playlist.dto.request.PlaylistSongAddRequest;
import com.vocaloidarchive.playlist.dto.response.PlaylistDetailResponse;
import com.vocaloidarchive.playlist.dto.response.PlaylistResponse;
import com.vocaloidarchive.playlist.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaylistController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class PlaylistControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean PlaylistService playlistService;

  private PlaylistResponse sampleList() {
    return new PlaylistResponse(1L, "My List", true, "alice", 3L, LocalDateTime.now());
  }

  private PlaylistDetailResponse sampleDetail() {
    return new PlaylistDetailResponse(1L, "My List", true, "alice", List.of(), LocalDateTime.now());
  }

  // ── GET /api/playlists ────────────────────────────────────────────────────

  @Test
  @WithMockUser
  void getMyPlaylists_authenticated_ok() throws Exception {
    given(playlistService.getMyPlaylists()).willReturn(List.of(sampleList()));

    mockMvc.perform(get("/api/playlists"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].title").value("My List"));
  }

  @Test
  void getMyPlaylists_anonymous_unauthorized() throws Exception {
    mockMvc.perform(get("/api/playlists"))
        .andExpect(status().isUnauthorized());
  }

  // ── POST /api/playlists ───────────────────────────────────────────────────

  @Test
  @WithMockUser
  void create_authenticated_ok() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("New List", true);
    given(playlistService.create(any())).willReturn(sampleList());

    mockMvc.perform(post("/api/playlists")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("My List"));
  }

  @Test
  @WithMockUser
  void create_validation_fail_blankTitle() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("", true);

    mockMvc.perform(post("/api/playlists")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void create_anonymous_unauthorized() throws Exception {
    PlaylistCreateRequest req = new PlaylistCreateRequest("List", true);

    mockMvc.perform(post("/api/playlists")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized());
  }

  // ── GET /api/playlists/{id} ───────────────────────────────────────────────

  @Test
  void getDetail_public_anonymous_ok() throws Exception {
    given(playlistService.getDetail(1L)).willReturn(sampleDetail());

    mockMvc.perform(get("/api/playlists/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("My List"));
  }

  @Test
  void getDetail_private_anonymous_forbidden() throws Exception {
    given(playlistService.getDetail(1L))
        .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

    mockMvc.perform(get("/api/playlists/1"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void getDetail_notFound() throws Exception {
    given(playlistService.getDetail(99L))
        .willThrow(new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

    mockMvc.perform(get("/api/playlists/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("PLAYLIST_NOT_FOUND"));
  }

  // ── DELETE /api/playlists/{id} ────────────────────────────────────────────

  @Test
  @WithMockUser
  void delete_owner_ok() throws Exception {
    willDoNothing().given(playlistService).delete(1L);

    mockMvc.perform(delete("/api/playlists/1").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void delete_forbidden() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(playlistService).delete(2L);

    mockMvc.perform(delete("/api/playlists/2").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void delete_anonymous_unauthorized() throws Exception {
    mockMvc.perform(delete("/api/playlists/1"))
        .andExpect(status().isUnauthorized());
  }

  // ── POST /api/playlists/{id}/songs ────────────────────────────────────────

  @Test
  @WithMockUser
  void addSong_authenticated_ok() throws Exception {
    PlaylistSongAddRequest req = new PlaylistSongAddRequest(5L);
    willDoNothing().given(playlistService).addSong(eq(1L), any());

    mockMvc.perform(post("/api/playlists/1/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void addSong_songNotFound() throws Exception {
    PlaylistSongAddRequest req = new PlaylistSongAddRequest(99L);
    willThrow(new BusinessException(ErrorCode.SONG_NOT_FOUND))
        .given(playlistService).addSong(eq(1L), any());

    mockMvc.perform(post("/api/playlists/1/songs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("SONG_NOT_FOUND"));
  }

  @Test
  void addSong_anonymous_unauthorized() throws Exception {
    mockMvc.perform(post("/api/playlists/1/songs")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"songId\":5}"))
        .andExpect(status().isUnauthorized());
  }

  // ── DELETE /api/playlists/{id}/songs/{songId} ─────────────────────────────

  @Test
  @WithMockUser
  void removeSong_owner_ok() throws Exception {
    willDoNothing().given(playlistService).removeSong(1L, 5L);

    mockMvc.perform(delete("/api/playlists/1/songs/5").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void removeSong_forbidden() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN))
        .given(playlistService).removeSong(2L, 5L);

    mockMvc.perform(delete("/api/playlists/2/songs/5").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void removeSong_anonymous_unauthorized() throws Exception {
    mockMvc.perform(delete("/api/playlists/1/songs/5"))
        .andExpect(status().isUnauthorized());
  }
}
