package com.vocaloidarchive.user.interfaces.dto.request;

import jakarta.validation.constraints.*;

public record SignUpRequest(
    @NotBlank
    @Size(min = 2, max = 20, message = "username은 2~20자여야 합니다")
    @Pattern(
        regexp = "^[가-힣a-zA-Z0-9_]+$",
        message = "username은 한글, 영문, 숫자, 밑줄(_)만 사용 가능합니다")
    String username,

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "email은 100자 이하여야 합니다")
    String email,

    @NotBlank
    @Size(min = 8, max = 72, message = "password는 8~72자여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
        message = "password는 영문과 숫자를 혼합하여 8자 이상 입력하세요")
    String password
) {}
