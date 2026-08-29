package com.zosh.notification.controller;

import com.zosh.modal.User;
import com.zosh.notification.dto.NotificationDTO;
import com.zosh.notification.service.NotificationService;
import com.zosh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                notificationService.getUserNotifications(
                        user.getId()
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                notificationService.getUnreadCount(
                        user.getId()
                )
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        notificationService.markAsRead(
                id,
                user.getId()
        );

        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName()
        );

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        notificationService.markAllAsRead(
                user.getId()
        );

        return ResponseEntity.ok().build();
    }
}