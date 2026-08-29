package com.zosh.notification.service.impl;

import com.zosh.fraud.modal.FraudAlert;
import com.zosh.modal.Branch;
import com.zosh.modal.Store;
import com.zosh.modal.User;
import com.zosh.notification.dto.NotificationDTO;
import com.zosh.notification.model.Notification;
import com.zosh.notification.repository.NotificationRepository;
import com.zosh.notification.service.NotificationService;
import com.zosh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void createFraudNotifications(FraudAlert fraudAlert) {

        Branch branch = fraudAlert.getBranch();

        Store store = branch.getStore();



        // -----------------------------
        // 1. Branch Manager
        // -----------------------------

        User branchManager = branch.getManager();

        if (branchManager != null) {

            saveFraudNotification(
                    branchManager,
                    fraudAlert
            );
        }

        // -----------------------------
        // 2. Store Admin
        // -----------------------------

        User storeAdmin = store.getStoreAdmin();

        if (storeAdmin != null) {

            saveFraudNotification(
                    storeAdmin,
                    fraudAlert
            );
        }
    }

    private void saveFraudNotification(
            User user,
            FraudAlert fraudAlert) {

        String title;

        switch (fraudAlert.getRiskLevel()) {

            case CRITICAL ->
                    title = "Critical Fraud Alert";

            case HIGH ->
                    title = "High Fraud Alert";

            case MEDIUM ->
                    title = "Medium Fraud Alert";

            default ->
                    title = "Fraud Alert";
        }

        String message =
                "Order #"
                        + fraudAlert.getOrder().getId()
                        + " was flagged as suspicious. "
                        + "Risk Score: "
                        + fraudAlert.getRiskScore()
                        + ". Reasons: "
                        + fraudAlert.getReason();

        Notification notification =
                Notification.builder()

                        .title(title)

                        .message(message)

                        .type("FRAUD")

                        .isRead(false)

                        .user(user)

                        .build();

        notificationRepository.save(notification);

    }
    @Override
    public List<NotificationDTO> getUserNotifications(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    @Override
    public long getUnreadCount(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return notificationRepository
                .countByUserAndIsReadFalse(user);
    }
    @Override
    @Transactional
    public void markAsRead(
            Long notificationId,
            Long userId) {

        notificationRepository.markAsRead(
                notificationId,
                userId
        );
    }
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {

        notificationRepository.markAllAsRead(userId);
    }
    private NotificationDTO toDTO(Notification notification) {

        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}