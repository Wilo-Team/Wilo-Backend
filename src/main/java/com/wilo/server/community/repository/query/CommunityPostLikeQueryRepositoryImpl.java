package com.wilo.server.community.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wilo.server.community.entity.CommunityPostLike;
import com.wilo.server.community.entity.QCommunityPost;
import com.wilo.server.community.entity.QCommunityPostLike;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommunityPostLikeQueryRepositoryImpl implements CommunityPostLikeQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCommunityPostLike postLike = QCommunityPostLike.communityPostLike;
    private static final QCommunityPost post = QCommunityPost.communityPost;

    @Override
    public List<CommunityPostLike> findLikedPostsByUserIdCursor(
            Long userId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(postLike)
                .join(postLike.post, post).fetchJoin()
                .where(
                        postLike.user.id.eq(userId),
                        cursorCondition(cursorCreatedAt, cursorId)
                )
                .orderBy(postLike.createdAt.desc(), postLike.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return postLike.createdAt.lt(cursorCreatedAt)
                .or(postLike.createdAt.eq(cursorCreatedAt).and(postLike.id.lt(cursorId)));
    }
}
