package com.vocaloidarchive.like.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.like.dto.response.LikeToggleResponse;
import com.vocaloidarchive.like.service.LikeService;
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

  private final LikeService likeService;

  @PostMapping("/{id}/like")
  public ResponseEntity<ApiResponse<LikeToggleResponse>> toggle(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(likeService.toggle(id)));
  }
}
