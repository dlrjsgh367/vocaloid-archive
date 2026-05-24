package com.vocaloidarchive.playlist.infra.persistence;

import com.vocaloidarchive.common.persistence.LazyAssociationGuards;
import com.vocaloidarchive.playlist.domain.Playlist;
import com.vocaloidarchive.user.infra.persistence.UserDomainMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct {@code PlaylistEntity → Playlist} mapper (시안 2 pattern).
 *
 * <p>The command path feeds {@code Playlist.isOwnedBy(...)}, so the owner must be present. Loads
 * that reconstitute a Playlist for ownership checks use
 * {@code PlaylistJpaRepository.findWithUserById} (@EntityGraph); the {@link LazyAssociationGuards}
 * guard then passes and {@code user} is materialized. A plain load would leave {@code user} null.
 */
@Mapper(componentModel = "spring", uses = {UserDomainMapper.class, LazyAssociationGuards.class})
public interface PlaylistDomainMapper {

  // Lombok's boolean getter isPublic() exposes the source property as "public", but the @Builder
  // target property is "isPublic" — map it explicitly so visibility isn't silently lost as false.
  @Mapping(target = "isPublic", source = "public")
  Playlist toDomain(PlaylistEntity entity);
}
