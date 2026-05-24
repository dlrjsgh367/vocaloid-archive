package com.vocaloidarchive.like.infra.persistence;

import com.vocaloidarchive.common.persistence.LazyAssociationGuards;
import com.vocaloidarchive.like.domain.Like;
import com.vocaloidarchive.song.infra.persistence.SongDomainMapper;
import com.vocaloidarchive.user.infra.persistence.UserDomainMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct {@code LikeEntity → Like} mapper (시안 2 pattern).
 *
 * <p>Both {@code user} and {@code song} associations are guarded by {@link LazyAssociationGuards}.
 * The toggle command path discards the {@code save} result, so the guard leaving these null on a
 * non-graph load is harmless; no {@code @EntityGraph} finder is required here.
 */
@Mapper(
    componentModel = "spring",
    uses = {UserDomainMapper.class, SongDomainMapper.class, LazyAssociationGuards.class})
public interface LikeDomainMapper {

  Like toDomain(LikeEntity entity);
}
