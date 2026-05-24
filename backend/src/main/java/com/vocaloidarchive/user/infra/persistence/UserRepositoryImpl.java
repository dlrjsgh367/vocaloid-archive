package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository jpa;
  private final UserDomainMapper mapper;

  @Override
  public User save(User user) {
    UserEntity saved = jpa.save(UserEntityMapper.toNewEntity(user));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<User> findById(Long id) {
    return jpa.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return jpa.findByEmail(email).map(mapper::toDomain);
  }

  @Override
  public boolean existsByUsername(String username) {
    return jpa.existsByUsername(username);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpa.existsByEmail(email);
  }
}
