package com.vocaloidarchive.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    ErrorCode errorCode = (ErrorCode) request.getAttribute("jwtError");
    if (errorCode == null) {
      errorCode = ErrorCode.INVALID_TOKEN;
    }
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    response.setStatus(errorCode.getHttpStatus().value());
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }
}
