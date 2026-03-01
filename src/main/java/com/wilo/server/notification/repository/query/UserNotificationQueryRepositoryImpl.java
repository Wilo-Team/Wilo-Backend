package com.wilo.server.notification.repository.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wilo.server.notification.entity.QUserNotification;
import com.wilo.server.notification.entity.UserNotification;
import com.wilo.server.user.entity.QUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserNotificationQueryRepositoryImpl implements UserNotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QUserNotification notification = QUserNotification.userNotification;
    private static final QUser actor = new QUser("actor");

    @Override
    public List<UserNotification> findLatestByReceiverWithCursor(
            Long receiverUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    ) {
        return queryFactory
                .selectFrom(notification)
                .join(notification.actorUser, actor).fetchJoin()
                .where(
                        notification.receiverUser.id.eq(receiverUserId),
                        cursorCondition(cursorCreatedAt, cursorId)
                )
                .orderBy(notification.createdAt.desc(), notification.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursorCreatedAt, Long cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return null;
        }

        return notification.createdAt.lt(cursorCreatedAt)
                .or(notification.createdAt.eq(cursorCreatedAt).and(notification.id.lt(cursorId)));
    }
}
