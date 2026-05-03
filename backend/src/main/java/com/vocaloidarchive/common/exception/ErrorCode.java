package com.vocaloidarchive.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 4xx
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값이 올바르지 않습니다"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
  SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "SONG_NOT_FOUND", "곡을 찾을 수 없습니다"),
  CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", "캐릭터를 찾을 수 없습니다"),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다"),
  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND", "플레이리스트를 찾을 수 없습니다"),
  DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 username입니다"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 email입니다"),

  // 5xx
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
