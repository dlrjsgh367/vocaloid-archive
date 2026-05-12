package com.vocaloidarchive.user.application;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.application.dto.command.SignUpCommand;
import com.vocaloidarchive.user.application.dto.result.UserResult;
import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpUseCase {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResult invoke(SignUpCommand cmd) {
    if (userRepository.existsByUsername(cmd.username())) {
      throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
    }
    if (userRepository.existsByEmail(cmd.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }
    User saved = userRepository.save(
        User.newSignup(cmd.username(), cmd.email(), passwordEncoder.encode(cmd.password())));
    return UserResult.from(saved);
  }
}
