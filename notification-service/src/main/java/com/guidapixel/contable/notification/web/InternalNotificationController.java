package com.guidapixel.contable.notification.web;

import com.guidapixel.contable.notification.service.NotificationService;
import com.guidapixel.contable.notification.web.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequest request) {
        try {
            notificationService.sendNotification(request);
            return ResponseEntity.ok(Map.of("status", "EXITO", "message", "Notificacion procesada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
