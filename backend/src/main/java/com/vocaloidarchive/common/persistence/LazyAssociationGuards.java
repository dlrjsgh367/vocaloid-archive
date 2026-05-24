package com.vocaloidarchive.common.persistence;

import com.vocaloidarchive.song.infra.persistence.SongEntity;
import com.vocaloidarchive.user.infra.persistence.UserEntity;
import org.hibernate.Hibernate;
import org.mapstruct.Condition;
import org.mapstruct.Mapper;

/**
 * Shared MapStruct {@code @Condition} guards for lazy JPA associations (시안 2 pattern).
 *
 * <p>Each guard is defined here exactly once and pulled into the domain mappers via
 * {@code uses = LazyAssociationGuards.class}. Defining them per-mapper instead causes MapStruct
 * "ambiguous presence check" errors when one mapper both declares a guard and {@code uses} another
 * mapper that declares the same-typed guard (e.g. CommentDomainMapper + SongDomainMapper, both
 * guarding {@code UserEntity}).
 *
 * <p>{@link Hibernate#isInitialized} only reads the proxy's initialized flag — no SELECT — so a
 * lazy association that wasn't fetched is skipped (left null) instead of triggering an N+1.
 */
@Mapper(componentModel = "spring")
public interface LazyAssociationGuards {

  @Condition
  default boolean isLoaded(UserEntity user) {
    return user != null && Hibernate.isInitialized(user);
  }

  @Condition
  default boolean isLoaded(SongEntity song) {
    return song != null && Hibernate.isInitialized(song);
  }
}
