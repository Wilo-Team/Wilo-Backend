package com.wilo.server.community.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityPost;
import com.wilo.server.community.entity.QCommunityPost;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommunityPostQueryRepositoryImpl implements CommunityPostQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCommunityPost post = QCommunityPost.communityPost;

    @Override
    public List<CommunityPost> findLatestPostsByCursor(
            CommunityCategory category,
            String keyword,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(post)
                .where(
                        categoryEq(category),
                        keywordContains(keyword),
                        latestCursorCondition(cursorCreatedAt, cursorId)
                )
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<CommunityPost> findRecommendedPostsByCursor(
            CommunityCategory category,
            String keyword,
            Long cursorLikeCount,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(post)
                .where(
                        categoryEq(category),
                        keywordContains(keyword),
                        recommendedCursorCondition(cursorLikeCount, cursorCreatedAt, cursorId)
                )
                .orderBy(post.likeCount.desc(), post.createdAt.desc(), post.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression categoryEq(CommunityCategory category) {
        if (category == null) {
            return null;
        }
        return post.category.eq(category);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalized = keyword.toLowerCase();
        return post.title.lower().contains(normalized)
                .or(post.content.lower().contains(normalized));
    }

    private BooleanExpression latestCursorCondition(LocalDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return post.createdAt.lt(cursorCreatedAt)
                .or(post.createdAt.eq(cursorCreatedAt).and(post.id.lt(cursorId)));
    }

    private BooleanExpression recommendedCursorCondition(
            Long cursorLikeCount,
            LocalDateTime cursorCreatedAt,
            Long cursorId
    ) {
        if (cursorLikeCount == null || cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return post.likeCount.lt(cursorLikeCount)
                .or(post.likeCount.eq(cursorLikeCount).and(post.createdAt.lt(cursorCreatedAt)))
                .or(post.likeCount.eq(cursorLikeCount)
                        .and(post.createdAt.eq(cursorCreatedAt))
                        .and(post.id.lt(cursorId)));
    }
}
