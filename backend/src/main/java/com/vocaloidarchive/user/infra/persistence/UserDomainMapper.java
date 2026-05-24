package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.user.domain.User;
import org.mapstruct.Mapper;

/**
 * MapStruct {@code UserEntity → User} mapper.
 *
 * <p>The shared root sub-mapper of the "시안 2" migration: every other domain's mapper references
 * this via {@code uses = UserDomainMapper.class} to materialize their embedded {@code User}. User
 * has no lazy associations of its own, so no {@code @Condition} guard is needed here.
 */
@Mapper(componentModel = "spring")
public interface UserDomainMapper {

  User toDomain(UserEntity entity);
}
