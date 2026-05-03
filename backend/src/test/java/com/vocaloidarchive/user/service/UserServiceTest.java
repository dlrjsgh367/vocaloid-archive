package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.domain.User;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock UserRepository userRepository;
  @Mock PasswordEncoder passwordEncoder;
  @InjectMocks UserService userService;

  @Test
  void givenValidRequest_whenSignUp_thenSavesUserAndReturnsResponse() {
    // given
    SignUpRequest request = new SignUpRequest("testuser", "test@example.com", "password1");
    given(userRepository.existsByUsername("testuser")).willReturn(false);
    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(passwordEncoder.encode("password1")).willReturn("hashed");
    User saved = User.of("testuser", "test@example.com", "hashed");
    given(userRepository.save(any(User.class))).willReturn(saved);

    // when
    UserResponse response = userService.signUp(request);

    // then
    assertThat(response.username()).isEqualTo("testuser");
    assertThat(response.email()).isEqualTo("test@example.com");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void givenDuplicateUsername_whenSignUp_thenThrowsDuplicateUsername() {
    // given
    SignUpRequest request = new SignUpRequest("taken", "new@example.com", "password1");
    given(userRepository.existsByUsername("taken")).willReturn(true);

    // when / then
    assertThatThrownBy(() -> userService.signUp(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.DUPLICATE_USERNAME));
  }

  @Test
  void givenDuplicateEmail_whenSignUp_thenThrowsDuplicateEmail() {
    // given
    SignUpRequest request = new SignUpRequest("newuser", "dup@example.com", "password1");
    given(userRepository.existsByUsername("newuser")).willReturn(false);
    given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

    // when / then
    assertThatThrownBy(() -> userService.signUp(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
  }

  @Test
  void givenValidCredentials_whenAuthenticate_thenReturnsUser() {
    // given
    User user = User.of("user", "user@example.com", "hashed");
    given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("password1", "hashed")).willReturn(true);

    // when
    User result = userService.authenticate("user@example.com", "password1");

    // then
    assertThat(result.getEmail()).isEqualTo("user@example.com");
  }

  @Test
  void givenUnknownEmail_whenAuthenticate_thenThrowsInvalidCredentials() {
    // given
    given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

    // when / then
    assertThatThrownBy(() -> userService.authenticate("no@example.com", "pw"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
  }

  @Test
  void givenWrongPassword_whenAuthenticate_thenThrowsInvalidCredentials() {
    // given
    User user = User.of("user", "user@example.com", "hashed");
    given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

    // when / then
    assertThatThrownBy(() -> userService.authenticate("user@example.com", "wrong"))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
  }
}
