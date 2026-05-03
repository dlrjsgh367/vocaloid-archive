package com.vocaloidarchive.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.response.ApiResponse;
import com.vocaloidarchive.common.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean JwtTokenProvider jwtTokenProvider;

  @Test
  void givenBusinessException_whenThrown_thenReturnsMappedHttpStatusAndErrorBody() throws Exception {
    mockMvc.perform(get("/dummy/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
  }

  @Test
  void givenInvalidPayload_whenPosted_thenReturnsValidationFailed() throws Exception {
    String body = objectMapper.writeValueAsString(new DummyRequest(""));

    mockMvc.perform(post("/dummy/echo").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  void givenUnknownException_whenThrown_thenReturnsInternalServerError() throws Exception {
    mockMvc.perform(get("/dummy/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
  }

  @RestController
  @RequestMapping("/dummy")
  static class DummyController {
    @GetMapping("/business-error")
    ApiResponse<Void> businessError() {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    @PostMapping("/echo")
    ApiResponse<String> echo(@RequestBody @Valid DummyRequest req) {
      return ApiResponse.success(req.value());
    }

    @GetMapping("/boom")
    ApiResponse<Void> boom() {
      throw new IllegalStateException("kaboom");
    }

    @PostMapping("/validate-multi")
    ApiResponse<String> validateMulti(@RequestBody @Valid MultiFieldRequest req) {
      return ApiResponse.success("ok");
    }

    @GetMapping("/bad-credentials")
    ApiResponse<Void> badCredentials() {
      throw new org.springframework.security.authentication.BadCredentialsException("bad");
    }
  }

  record DummyRequest(@NotBlank String value) {}

  record MultiFieldRequest(@NotBlank String name, @jakarta.validation.constraints.Email String email) {}

  @Test
  void givenMalformedJson_whenPosted_thenReturns400WithApiResponse() throws Exception {
    mockMvc.perform(post("/dummy/echo")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{invalid-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void givenMultipleValidationErrors_whenPosted_thenDetailsIsMap() throws Exception {
    String body = objectMapper.writeValueAsString(new MultiFieldRequest("", "not-an-email"));
    mockMvc.perform(post("/dummy/validate-multi")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.error.details.name").exists())
        .andExpect(jsonPath("$.error.details.email").exists());
  }

  @Test
  void givenBadCredentialsException_whenThrown_thenReturns401InvalidCredentials() throws Exception {
    mockMvc.perform(get("/dummy/bad-credentials"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
  }
}
