package com.vocaloidarchive.song.infra.persistence;

import com.vocaloidarchive.common.persistence.LazyAssociationGuards;
import com.vocaloidarchive.song.domain.Song;
import com.vocaloidarchive.user.infra.persistence.UserDomainMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct {@code SongEntity → Song} mapper (시안 2 pattern).
 *
 * <p>The {@code registeredBy} association is materialized through {@link UserDomainMapper} only when
 * it has actually been fetched — the {@link LazyAssociationGuards} {@code @Condition} guards the
 * lazy proxy so a plain {@code findById} leaves {@code registeredBy} null instead of triggering an
 * N+1 SELECT. Use {@code SongJpaRepository}'s @EntityGraph finder when a populated user is needed.
 */
@Mapper(componentModel = "spring", uses = {UserDomainMapper.class, LazyAssociationGuards.class})
public interface SongDomainMapper {

  Song toDomain(SongEntity entity);
}
