package com.wilo.server.notification.repository.query;

import com.wilo.server.notification.entity.UserNotification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface UserNotificationQueryRepository {

    List<UserNotification> findLatestByReceiverWithCursor(
            Long receiverUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Pageable pageable
    );
}
