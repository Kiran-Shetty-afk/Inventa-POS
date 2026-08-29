package com.zosh.notification.service;

import com.zosh.fraud.modal.FraudAlert;
import com.zosh.notification.dto.NotificationDTO;
import com.zosh.notification.model.Notification;

import java.util.List;

public interface NotificationService {

    void createFraudNotifications(FraudAlert fraudAlert);

    List<NotificationDTO> getUserNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}