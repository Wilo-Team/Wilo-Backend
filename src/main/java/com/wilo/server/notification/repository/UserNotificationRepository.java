package com.wilo.server.notification.repository;

import com.wilo.server.notification.entity.UserNotification;
import com.wilo.server.notification.repository.query.UserNotificationQueryRepository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long>, UserNotificationQueryRepository {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserNotification n
               set n.isRead = true,
                   n.readAt = :readAt
             where n.receiverUser.id = :receiverUserId
               and n.isRead = false
            """)
    int markAllAsRead(@Param("receiverUserId") Long receiverUserId, @Param("readAt") LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from UserNotification n
             where n.receiverUser.id = :receiverUserId
            """)
    int deleteAllByReceiverUserId(@Param("receiverUserId") Long receiverUserId);
}
