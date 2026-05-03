package com.vocaloidarchive.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void givenData_whenSuccess_thenSuccessTrueAndDataPopulated() throws Exception {
    ApiResponse<String> response = ApiResponse.success("hello");

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"success\":true");
    assertThat(json).contains("\"data\":\"hello\"");
    assertThat(json).contains("\"error\":null");
  }

  @Test
  void givenErrorCodeAndMessage_whenError_thenSuccessFalseAndErrorPopulated() throws Exception {
    ApiResponse<Void> response = ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다");

    String json = objectMapper.writeValueAsString(response);

    assertThat(json).contains("\"success\":false");
    assertThat(json).contains("\"data\":null");
    assertThat(json).contains("\"code\":\"USER_NOT_FOUND\"");
    assertThat(json).contains("\"message\":\"사용자를 찾을 수 없습니다\"");
  }

  @Test
  void errorWithDetails_includesDetailsMap() {
    var details = java.util.Map.of("email", "올바른 이메일 형식이 아닙니다");
    var response = ApiResponse.errorWithDetails("VALIDATION_FAILED", "입력값이 올바르지 않습니다", details);

    assertThat(response.success()).isFalse();
    assertThat(response.error().details()).containsEntry("email", "올바른 이메일 형식이 아닙니다");
  }

  @Test
  void error_withoutDetails_detailsIsNull() {
    var response = ApiResponse.error("SOME_CODE", "some message");
    assertThat(response.error().details()).isNull();
  }
}
