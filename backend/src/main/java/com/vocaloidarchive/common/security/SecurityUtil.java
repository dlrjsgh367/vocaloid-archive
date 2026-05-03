package com.vocaloidarchive.common.security;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

  public Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()
        || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }
    return userDetails.getUserId();
  }
}
