package com.vocaloidarchive.song.infra.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vocaloidarchive.character.infra.persistence.QCharacterEntity;
import com.vocaloidarchive.common.response.PageResponse;
import com.vocaloidarchive.common.util.YoutubeUtil;
import com.vocaloidarchive.like.infra.persistence.QLikeEntity;
import com.vocaloidarchive.song.application.SongSortKey;
import com.vocaloidarchive.song.application.dto.result.SongDetailResult;
import com.vocaloidarchive.song.application.dto.result.SongResult;
import com.vocaloidarchive.song.application.port.SongQueryRepository;
import com.vocaloidarchive.song.domain.Mood;
import com.vocaloidarchive.tag.infra.persistence.QTagEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class SongQueryRepositoryImpl implements SongQueryRepository {

  private static final QSongEntity S = QSongEntity.songEntity;
  private static final QSongCharacterEntity SC = QSongCharacterEntity.songCharacterEntity;
  private static final QSongTagEntity ST = QSongTagEntity.songTagEntity;
  private static final QCharacterEntity C = QCharacterEntity.characterEntity;
  private static final QTagEntity T = QTagEntity.tagEntity;
  private static final QLikeEntity L = QLikeEntity.likeEntity;

  private final JPAQueryFactory queryFactory;
  private final SongJpaRepository songJpa;

  public SongQueryRepositoryImpl(JPAQueryFactory queryFactory, SongJpaRepository songJpa) {
    this.queryFactory = queryFactory;
    this.songJpa = songJpa;
  }

  @Override
  public PageResponse<SongResult> search(
      String keyword, Mood mood, Long characterId, Long tagId,
      SongSortKey sort, Pageable pageable) {
    BooleanBuilder where = new BooleanBuilder();

    if (keyword != null && !keyword.isBlank()) {
      where.and(S.title.containsIgnoreCase(keyword)
          .or(JPAExpressions.selectOne().from(SC)
              .innerJoin(SC.character, C)
              .where(SC.song.eq(S).and(C.name.containsIgnoreCase(keyword))).exists())
          .or(JPAExpressions.selectOne().from(ST)
              .innerJoin(ST.tag, T)
              .where(ST.song.eq(S).and(T.name.containsIgnoreCase(keyword))).exists()));
    }
    if (mood != null) {
      where.and(S.mood.eq(mood));
    }
    if (characterId != null) {
      where.and(JPAExpressions.selectOne().from(SC)
          .where(SC.song.eq(S).and(SC.character.id.eq(characterId))).exists());
    }
    if (tagId != null) {
      where.and(JPAExpressions.selectOne().from(ST)
          .where(ST.song.eq(S).and(ST.tag.id.eq(tagId))).exists());
    }

    Expression<Long> likeCountSubquery =
        JPAExpressions.select(L.count()).from(L).where(L.song.eq(S));
    NumberExpression<Long> likeCountExpr =
        Expressions.numberTemplate(Long.class, "({0})", likeCountSubquery);

    OrderSpecifier<?>[] order = orderFor(sort, likeCountExpr);

    List<SongEntity> rows = queryFactory.selectFrom(S).distinct()
        .where(where)
        .orderBy(order)
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long total = queryFactory.select(S.countDistinct())
        .from(S).where(where).fetchOne();

    List<Long> songIds = rows.stream().map(SongEntity::getId).toList();
    Map<Long, Long> likeCounts = likeCountsFor(songIds);

    List<SongResult> content = rows.stream()
        .map(s -> toSongResult(s, likeCounts.getOrDefault(s.getId(), 0L)))
        .toList();

    return PageResponse.from(new PageImpl<>(content, pageable, total == null ? 0 : total));
  }

  @Override
  public Optional<SongDetailResult> findDetailById(Long id) {
    Optional<SongEntity> opt = songJpa.findDetailWithCharacters(id);
    if (opt.isEmpty()) return Optional.empty();
    SongEntity s = opt.get();
    songJpa.findDetailWithTags(id);
    long likeCount = likeCountFor(id);
    return Optional.of(toSongDetailResult(s, likeCount));
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private SongResult toSongResult(SongEntity s, long likeCount) {
    SongResult.Owner owner = new SongResult.Owner(
        s.getRegisteredBy().getId(), s.getRegisteredBy().getUsername());
    List<SongResult.CharacterRef> characters = s.getCharacters().stream()
        .map(sc -> new SongResult.CharacterRef(
            sc.getCharacter().getId(),
            sc.getCharacter().getName(),
            sc.getCharacter().getColorHex(),
            sc.getCharacter().getImageUrl()))
        .toList();
    List<String> tags = s.getTags().stream()
        .map(st -> st.getTag().getName())
        .toList();
    return new SongResult(s.getId(), s.getTitle(),
        YoutubeUtil.resolveThumbnailUrl(s.getThumbnailUrl(), s.getYoutubeUrl()), s.getMood(),
        s.getPlayCount(), likeCount, owner, characters, tags, s.getCreatedAt());
  }

  private SongDetailResult toSongDetailResult(SongEntity s, long likeCount) {
    SongResult.Owner owner = new SongResult.Owner(
        s.getRegisteredBy().getId(), s.getRegisteredBy().getUsername());
    List<SongResult.CharacterRef> characters = s.getCharacters().stream()
        .map(sc -> new SongResult.CharacterRef(
            sc.getCharacter().getId(),
            sc.getCharacter().getName(),
            sc.getCharacter().getColorHex(),
            sc.getCharacter().getImageUrl()))
        .toList();
    List<String> tags = s.getTags().stream()
        .map(st -> st.getTag().getName())
        .toList();
    return new SongDetailResult(s.getId(), s.getTitle(), s.getYoutubeUrl(), s.getNiconicoUrl(),
        YoutubeUtil.resolveThumbnailUrl(s.getThumbnailUrl(), s.getYoutubeUrl()),
        s.getBpm(), s.getMood(), s.getPlayCount(), likeCount,
        owner, characters, tags, s.getCreatedAt());
  }

  private Map<Long, Long> likeCountsFor(List<Long> songIds) {
    if (songIds == null || songIds.isEmpty()) return Map.of();
    java.util.Map<Long, Long> result = new java.util.HashMap<>();
    queryFactory.select(L.song.id, L.count())
        .from(L).where(L.song.id.in(songIds))
        .groupBy(L.song.id)
        .fetch()
        .forEach(t -> result.put(t.get(0, Long.class), t.get(1, Long.class)));
    songIds.forEach(id -> result.putIfAbsent(id, 0L));
    return result;
  }

  private long likeCountFor(Long songId) {
    Long c = queryFactory.select(L.count()).from(L).where(L.song.id.eq(songId)).fetchOne();
    return c == null ? 0L : c;
  }

  private OrderSpecifier<?>[] orderFor(SongSortKey sort, NumberExpression<Long> likeCountExpr) {
    return switch (sort) {
      case POPULAR -> new OrderSpecifier<?>[]{ likeCountExpr.desc(), S.createdAt.desc(), S.id.desc() };
      case PLAYED  -> new OrderSpecifier<?>[]{ S.playCount.desc(), S.createdAt.desc(), S.id.desc() };
      case LATEST  -> new OrderSpecifier<?>[]{ S.createdAt.desc(), S.id.desc() };
    };
  }
}
