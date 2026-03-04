package com.wilo.server.community.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wilo.server.community.entity.CommunitySearchHistory;
import com.wilo.server.community.entity.QCommunitySearchHistory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommunitySearchHistoryQueryRepositoryImpl implements CommunitySearchHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCommunitySearchHistory history = QCommunitySearchHistory.communitySearchHistory;

    @Override
    public List<CommunitySearchHistory> findLatestByUserIdCursor(
            Long userId,
            LocalDateTime cursorLastSearchedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(history)
                .where(
                        history.user.id.eq(userId),
                        cursorCondition(cursorLastSearchedAt, cursorId)
                )
                .orderBy(history.lastSearchedAt.desc(), history.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursorLastSearchedAt, Long cursorId) {
        if (cursorLastSearchedAt == null || cursorId == null) {
            return null;
        }

        return history.lastSearchedAt.lt(cursorLastSearchedAt)
                .or(history.lastSearchedAt.eq(cursorLastSearchedAt).and(history.id.lt(cursorId)));
    }
}
