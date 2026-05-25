package com.vocaloidarchive.common.security;

import com.vocaloidarchive.user.application.port.UserRepository;
import com.vocaloidarchive.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    User user = userRepository.findByEmail(normalized)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
    return new CustomUserDetails(user.getId());
  }
}
