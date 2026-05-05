package com.vocaloidarchive.character.controller;

import com.vocaloidarchive.character.dto.response.CharacterResponse;
import com.vocaloidarchive.character.service.CharacterService;
import com.vocaloidarchive.common.config.SecurityConfig;
import com.vocaloidarchive.common.security.CustomUserDetailsService;
import com.vocaloidarchive.common.security.JwtAccessDeniedHandler;
import com.vocaloidarchive.common.security.JwtAuthenticationEntryPoint;
import com.vocaloidarchive.common.security.JwtAuthenticationFilter;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CharacterController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class
})
class CharacterControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean JwtTokenProvider jwtTokenProvider;
  @MockBean CustomUserDetailsService customUserDetailsService;
  @MockBean CharacterService characterService;

  @Test
  @DisplayName("GET /api/characters: 인증 없이 200 반환, 캐릭터 목록 포함")
  void findAll_returnsCharacterList_anonymously() throws Exception {
    given(characterService.findAll()).willReturn(List.of(
        new CharacterResponse(1L, "하츠네 미쿠", "#39C5BB", null),
        new CharacterResponse(2L, "KAITO", "#1E90FF", null)));

    mockMvc.perform(get("/api/characters"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].name").value("하츠네 미쿠"))
        .andExpect(jsonPath("$.data[1].name").value("KAITO"));
  }
}
