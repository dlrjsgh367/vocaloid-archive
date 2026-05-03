package com.vocaloidarchive.common.exception;

import com.vocaloidarchive.common.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
    ErrorCode code = ex.getErrorCode();
    log.warn("BusinessException: code={}, message={}", code.getCode(), ex.getMessage());
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.INVALID_CREDENTIALS.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INVALID_CREDENTIALS.getCode(),
            ErrorCode.INVALID_CREDENTIALS.getMessage()));
  }

  @ExceptionHandler(ExpiredJwtException.class)
  public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.EXPIRED_TOKEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.EXPIRED_TOKEN.getCode(),
            ErrorCode.EXPIRED_TOKEN.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.INVALID_TOKEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INVALID_TOKEN.getCode(),
            ErrorCode.INVALID_TOKEN.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.FORBIDDEN.getCode(),
            ErrorCode.FORBIDDEN.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값",
            (first, second) -> first));
    log.warn("Validation failed: {}", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.errorWithDetails(
            ErrorCode.VALIDATION_FAILED.getCode(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            details));
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("Spring MVC exception [{}]: {}", status, ex.getMessage());
    ErrorCode errorCode = status.is4xxClientError()
        ? ErrorCode.VALIDATION_FAILED
        : ErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status)
        .headers(headers)
        .body(ApiResponse.error(
            errorCode.getCode(),
            ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다"));
  }
}
