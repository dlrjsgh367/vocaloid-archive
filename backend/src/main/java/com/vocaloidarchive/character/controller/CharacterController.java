package com.vocaloidarchive.character.controller;

import com.vocaloidarchive.character.dto.response.CharacterResponse;
import com.vocaloidarchive.character.service.CharacterService;
import com.vocaloidarchive.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

  private final CharacterService characterService;

  @GetMapping
  public ApiResponse<List<CharacterResponse>> findAll() {
    return ApiResponse.success(characterService.findAll());
  }
}
