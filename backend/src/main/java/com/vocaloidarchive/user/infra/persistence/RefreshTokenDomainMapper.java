package com.vocaloidarchive.user.infra.persistence;

import com.vocaloidarchive.common.persistence.LazyAssociationGuards;
import com.vocaloidarchive.user.domain.RefreshToken;
import org.mapstruct.Mapper;

/** MapStruct {@code RefreshTokenEntity → RefreshToken} mapper with a guarded {@code user}. */
@Mapper(componentModel = "spring", uses = {UserDomainMapper.class, LazyAssociationGuards.class})
public interface RefreshTokenDomainMapper {

  RefreshToken toDomain(RefreshTokenEntity entity);
}
