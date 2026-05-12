package com.vocaloidarchive.song.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vocaloidarchive.character.infra.persistence.QCharacterEntity;
import com.vocaloidarchive.like.infra.persistence.QLikeEntity;
import com.vocaloidarchive.song.dto.request.SongSearchRequest;
import com.vocaloidarchive.song.dto.request.SongSort;
import com.vocaloidarchive.tag.infra.persistence.QTagEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SongQueryRepositoryImpl {

  private static final QSongEntity S = QSongEntity.songEntity;
  private static final QSongCharacterEntity SC = QSongCharacterEntity.songCharacterEntity;
  private static final QSongTagEntity ST = QSongTagEntity.songTagEntity;
  private static final QCharacterEntity C = QCharacterEntity.characterEntity;
  private static final QTagEntity T = QTagEntity.tagEntity;
  private static final QLikeEntity L = QLikeEntity.likeEntity;

  private final JPAQueryFactory queryFactory;

  public SongQueryRepositoryImpl(JPAQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  public Page<SongEntity> search(SongSearchRequest req, Pageable pageable) {
    BooleanBuilder where = new BooleanBuilder();

    if (req.keyword() != null && !req.keyword().isBlank()) {
      String kw = req.keyword();
      where.and(S.title.containsIgnoreCase(kw)
          .or(JPAExpressions.selectOne().from(SC)
              .innerJoin(SC.character, C)
              .where(SC.song.eq(S).and(C.name.containsIgnoreCase(kw))).exists())
          .or(JPAExpressions.selectOne().from(ST)
              .innerJoin(ST.tag, T)
              .where(ST.song.eq(S).and(T.name.containsIgnoreCase(kw))).exists()));
    }
    if (req.mood() != null) {
      where.and(S.mood.eq(req.mood()));
    }
    if (req.characterId() != null) {
      where.and(JPAExpressions.selectOne().from(SC)
          .where(SC.song.eq(S).and(SC.character.id.eq(req.characterId()))).exists());
    }
    if (req.tagId() != null) {
      where.and(JPAExpressions.selectOne().from(ST)
          .where(ST.song.eq(S).and(ST.tag.id.eq(req.tagId()))).exists());
    }

    Expression<Long> likeCountSubquery =
        JPAExpressions.select(L.count()).from(L).where(L.song.eq(S));
    NumberExpression<Long> likeCountExpr =
        Expressions.numberTemplate(Long.class, "({0})", likeCountSubquery);

    OrderSpecifier<?>[] order = orderFor(req.sortOrDefault(), likeCountExpr);

    List<SongEntity> rows = queryFactory.selectFrom(S).distinct()
        .where(where)
        .orderBy(order)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long total = queryFactory.select(S.countDistinct())
        .from(S).where(where).fetchOne();

    return new PageImpl<>(rows, pageable, total == null ? 0 : total);
  }

  public Map<Long, Long> likeCountsFor(List<Long> songIds) {
    if (songIds == null || songIds.isEmpty()) return Map.of();
    Map<Long, Long> result = new HashMap<>();
    queryFactory.select(L.song.id, L.count())
        .from(L).where(L.song.id.in(songIds))
        .groupBy(L.song.id)
        .fetch()
        .forEach(t -> result.put(t.get(0, Long.class), t.get(1, Long.class)));
    songIds.forEach(id -> result.putIfAbsent(id, 0L));
    return result;
  }

  public long likeCountFor(Long songId) {
    Long c = queryFactory.select(L.count()).from(L).where(L.song.id.eq(songId)).fetchOne();
    return c == null ? 0L : c;
  }

  private OrderSpecifier<?>[] orderFor(SongSort sort, NumberExpression<Long> likeCountExpr) {
    return switch (sort) {
      case POPULAR -> new OrderSpecifier<?>[]{ likeCountExpr.desc(), S.createdAt.desc(), S.id.desc() };
      case PLAYED  -> new OrderSpecifier<?>[]{ S.playCount.desc(), S.createdAt.desc(), S.id.desc() };
      case LATEST  -> new OrderSpecifier<?>[]{ S.createdAt.desc(), S.id.desc() };
    };
  }
}
