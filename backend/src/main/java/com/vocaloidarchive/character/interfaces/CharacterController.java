package com.vocaloidarchive.character.interfaces;

import com.vocaloidarchive.character.application.GetCharactersUseCase;
import com.vocaloidarchive.character.interfaces.dto.response.CharacterResponse;
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

  private final GetCharactersUseCase getCharactersUseCase;

  @GetMapping
  public ApiResponse<List<CharacterResponse>> findAll() {
    return ApiResponse.success(
        getCharactersUseCase.invoke().stream()
            .map(CharacterResponse::from)
            .toList());
  }
}
