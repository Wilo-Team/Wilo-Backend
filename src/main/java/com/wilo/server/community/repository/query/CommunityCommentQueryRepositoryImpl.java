package com.wilo.server.community.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wilo.server.community.entity.comment.CommunityComment;
import com.wilo.server.community.entity.comment.QCommunityComment;
import com.wilo.server.community.entity.post.QCommunityPost;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommunityCommentQueryRepositoryImpl implements CommunityCommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCommunityComment comment = QCommunityComment.communityComment;
    private static final QCommunityPost post = QCommunityPost.communityPost;

    @Override
    public List<CommunityComment> findLatestByUserIdCursor(
            Long userId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(comment)
                .join(comment.post, post).fetchJoin()
                .where(
                        comment.user.id.eq(userId),
                        cursorCondition(cursorCreatedAt, cursorId)
                )
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return comment.createdAt.lt(cursorCreatedAt)
                .or(comment.createdAt.eq(cursorCreatedAt).and(comment.id.lt(cursorId)));
    }
}
