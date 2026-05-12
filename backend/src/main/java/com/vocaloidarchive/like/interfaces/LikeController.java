package com.vocaloidarchive.like.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.like.application.ToggleLikeUseCase;
import com.vocaloidarchive.like.interfaces.dto.response.LikeToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class LikeController {

  private final ToggleLikeUseCase toggleLikeUseCase;

  @PostMapping("/{id}/like")
  public ResponseEntity<ApiResponse<LikeToggleResponse>> toggle(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(
        LikeToggleResponse.from(toggleLikeUseCase.invoke(id))));
  }
}
