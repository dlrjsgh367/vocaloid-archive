package com.vocaloidarchive.user.service;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.user.dto.request.SignUpRequest;
import com.vocaloidarchive.user.dto.response.UserResponse;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import com.vocaloidarchive.user.infra.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserJpaRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse signUp(SignUpRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
    }
    UserEntity user = UserEntity.of(
        request.username(),
        request.email(),
        passwordEncoder.encode(request.password()));
    userRepository.save(user);
    return UserResponse.from(user);
  }

  public UserEntity authenticate(String email, String password) {
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    return user;
  }
}
