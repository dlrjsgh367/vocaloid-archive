package com.vocaloidarchive.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessExceptionTest {

  @Test
  void givenErrorCode_whenConstructed_thenCarriesErrorCode() {
    BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    assertThat(ex.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
  }

  @Test
  void givenErrorCodeUserNotFound_whenInspected_thenStatusIs404() {
    assertThat(ErrorCode.USER_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ErrorCode.USER_NOT_FOUND.getCode()).isEqualTo("USER_NOT_FOUND");
  }

  @Test
  void givenBusinessException_whenThrown_thenIsRuntimeException() {
    assertThatThrownBy(() -> { throw new BusinessException(ErrorCode.FORBIDDEN); })
        .isInstanceOf(RuntimeException.class);
  }
}
