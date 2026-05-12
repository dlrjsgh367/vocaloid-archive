package com.vocaloidarchive.song.interfaces.dto.response;

import com.vocaloidarchive.song.application.dto.result.SongResult;

public record UserSummary(Long id, String username) {

  public static UserSummary from(SongResult.Owner owner) {
    return new UserSummary(owner.id(), owner.username());
  }
}
