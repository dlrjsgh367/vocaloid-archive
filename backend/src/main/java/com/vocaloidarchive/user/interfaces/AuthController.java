package com.vocaloidarchive.user.interfaces;

import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.user.application.AuthenticateUseCase;
import com.vocaloidarchive.user.application.IssueTokensUseCase;
import com.vocaloidarchive.user.application.RevokeRefreshTokenUseCase;
import com.vocaloidarchive.user.application.RotateRefreshTokenUseCase;
import com.vocaloidarchive.user.application.SignUpUseCase;
import com.vocaloidarchive.user.application.dto.command.AuthenticateCommand;
import com.vocaloidarchive.user.application.dto.command.SignUpCommand;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.interfaces.dto.request.LoginRequest;
import com.vocaloidarchive.user.interfaces.dto.request.RefreshRequest;
import com.vocaloidarchive.user.interfaces.dto.request.SignUpRequest;
import com.vocaloidarchive.user.interfaces.dto.response.TokenResponse;
import com.vocaloidarchive.user.interfaces.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SignUpUseCase signUpUseCase;
  private final AuthenticateUseCase authenticateUseCase;
  private final IssueTokensUseCase issueTokensUseCase;
  private final RotateRefreshTokenUseCase rotateRefreshTokenUseCase;
  private final RevokeRefreshTokenUseCase revokeRefreshTokenUseCase;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UserResponse> signUp(@RequestBody @Valid SignUpRequest req) {
    var result = signUpUseCase.invoke(new SignUpCommand(req.username(), req.email(), req.password()));
    return ApiResponse.success(UserResponse.from(result));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
    User user = authenticateUseCase.invoke(new AuthenticateCommand(req.email(), req.password()));
    return ApiResponse.success(TokenResponse.from(issueTokensUseCase.invoke(user)));
  }

  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
    return ApiResponse.success(TokenResponse.from(rotateRefreshTokenUseCase.invoke(req.refreshToken())));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest req) {
    revokeRefreshTokenUseCase.invoke(req.refreshToken());
    return ApiResponse.success(null);
  }
}
