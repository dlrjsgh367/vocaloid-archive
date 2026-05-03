package com.vocaloidarchive.user.controller;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.user.dto.request.LoginRequest;
import com.vocaloidarchive.user.dto.request.RefreshRequest;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.TokenResponse;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.service.RefreshTokenService;
import com.vocaloidarchive.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final RefreshTokenService refreshTokenService;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> signUp(@RequestBody @Valid SignUpRequest request) {
    return ApiResponse.success(userService.signUp(request));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
    var user = userService.authenticate(request.email(), request.password());
    return ApiResponse.success(refreshTokenService.issueTokens(user));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
    return ApiResponse.success(refreshTokenService.rotate(request.refreshToken()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
    refreshTokenService.revoke(request.refreshToken());
    return ApiResponse.success(null);
  }
}
