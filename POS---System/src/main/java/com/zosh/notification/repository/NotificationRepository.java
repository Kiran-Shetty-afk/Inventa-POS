package com.zosh.notification.repository;

import com.zosh.notification.model.Notification;
import com.zosh.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.id = :notificationId
        AND n.user.id = :userId
    """)
    int markAsRead(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.user.id = :userId
        AND n.isRead = false
    """)
    int markAllAsRead(
            @Param("userId") Long userId
    );
    @Modifying
    @Query("""
    DELETE FROM Notification n
    WHERE n.user.id IN :userIds
""")
    int deleteByUserIds(@Param("userIds") List<Long> userIds);
}