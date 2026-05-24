package com.vocaloidarchive.comment.infra.persistence;

import com.vocaloidarchive.comment.domain.Comment;
import com.vocaloidarchive.common.persistence.LazyAssociationGuards;
import com.vocaloidarchive.song.infra.persistence.SongDomainMapper;
import com.vocaloidarchive.user.infra.persistence.UserDomainMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct {@code CommentEntity → Comment} mapper (시안 2 pattern).
 *
 * <p>Both the {@code user} and {@code song} associations are guarded by {@link LazyAssociationGuards}.
 * The command path (save → domain) only reads id/content/createdAt off the result, so leaving these
 * null on a non-graph load is harmless; no {@code @EntityGraph} finder is required here.
 */
@Mapper(
    componentModel = "spring",
    uses = {UserDomainMapper.class, SongDomainMapper.class, LazyAssociationGuards.class})
public interface CommentDomainMapper {

  Comment toDomain(CommentEntity entity);
}
