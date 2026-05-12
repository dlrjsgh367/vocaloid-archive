package com.vocaloidarchive.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import com.vocaloidarchive.user.dto.request.LoginRequest;
import com.vocaloidarchive.user.dto.request.RefreshRequest;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.interfaces.AuthController;
import com.vocaloidarchive.user.service.RefreshTokenService;
import com.vocaloidarchive.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("phase-7-migration: rewrite in Phase 7-8")
@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class AuthControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean UserService userService;
  @MockBean RefreshTokenService refreshTokenService;

  private static final String VALID_TOKEN = "valid.jwt.token";

  @BeforeEach
  void setUp() {
    given(jwtTokenProvider.getUserIdFromToken(VALID_TOKEN)).willReturn(1L);
  }

  @Test
  void givenValidSignUpRequest_whenSignUp_thenReturns201() throws Exception {
    SignUpRequest req = new SignUpRequest("testuser", "test@example.com", "password1");
    UserResponse resp = new UserResponse(1L, "testuser", "test@example.com", null, LocalDateTime.now());
    given(userService.signUp(any())).willReturn(resp);

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.username").value("testuser"));
  }

  @Test
  void givenInvalidSignUpRequest_whenSignUp_thenReturns400WithDetailsMap() throws Exception {
    SignUpRequest req = new SignUpRequest("", "not-email", "short");

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.error.details").isMap());
  }

  @Test
  void givenValidLoginRequest_whenLogin_thenReturns200WithTokens() throws Exception {
    LoginRequest req = new LoginRequest("test@example.com", "password1");
    UserEntity user = UserEntity.of("user", "test@example.com", "hash");
    TokenResponse tokens = new TokenResponse("access.token", "raw-refresh");
    given(userService.authenticate("test@example.com", "password1")).willReturn(user);
    given(refreshTokenService.issueTokens(any(UserEntity.class))).willReturn(tokens);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("access.token"))
        .andExpect(jsonPath("$.data.refreshToken").value("raw-refresh"));
  }

  @Test
  void givenWrongCredentials_whenLogin_thenReturns401InvalidCredentials() throws Exception {
    LoginRequest req = new LoginRequest("test@example.com", "wrong");
    given(userService.authenticate(anyString(), anyString()))
        .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void givenValidRefreshToken_whenRefresh_thenReturns200WithNewTokens() throws Exception {
    RefreshRequest req = new RefreshRequest("old-refresh-token");
    TokenResponse tokens = new TokenResponse("new.access", "new-refresh");
    given(refreshTokenService.rotate("old-refresh-token")).willReturn(tokens);

    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken").value("new.access"));
  }

  @Test
  void givenNoBearerToken_whenLogout_thenReturns401() throws Exception {
    RefreshRequest req = new RefreshRequest("some-refresh");

    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  @Test
  void givenValidBearer_whenLogout_thenReturns200() throws Exception {
    RefreshRequest req = new RefreshRequest("some-refresh");
    willDoNothing().given(refreshTokenService).revoke(anyString());

    mockMvc.perform(post("/api/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}
