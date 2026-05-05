package com.vocaloidarchive.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.comment.dto.request.CommentCreateRequest;
import com.vocaloidarchive.comment.dto.response.CommentResponse;
import com.vocaloidarchive.comment.service.CommentService;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class CommentControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean CommentService commentService;

  private CommentResponse sampleComment() {
    return new CommentResponse(1L, "great song", "alice", LocalDateTime.now());
  }

  @Test
  void listComments_anonymous_returns200() throws Exception {
    PageResponse<CommentResponse> page = PageResponse.from(
        new PageImpl<>(List.of(sampleComment()), PageRequest.of(0, 20), 1));
    given(commentService.list(eq(1L), any())).willReturn(page);

    mockMvc.perform(get("/api/songs/1/comments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].content").value("great song"));
  }

  @Test
  @WithMockUser
  void createComment_authenticated_returns201() throws Exception {
    given(commentService.create(eq(1L), any())).willReturn(sampleComment());

    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest("great song"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.content").value("great song"));
  }

  @Test
  void createComment_unauthenticated_returns401() throws Exception {
    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest("text"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void createComment_blankContent_returns400() throws Exception {
    mockMvc.perform(post("/api/songs/1/comments").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CommentCreateRequest(""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  @WithMockUser
  void deleteComment_owner_returns204() throws Exception {
    willDoNothing().given(commentService).delete(1L);

    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteComment_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void deleteComment_notOwner_returns403() throws Exception {
    willThrow(new BusinessException(ErrorCode.FORBIDDEN)).given(commentService).delete(1L);

    mockMvc.perform(delete("/api/comments/1").with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }
}
