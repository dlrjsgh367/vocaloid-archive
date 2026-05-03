package com.vocaloidarchive.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocaloidarchive.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
  }

  record DummyRequest(@NotBlank String value) {}
}
